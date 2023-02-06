package org.qiunet.flash.handler.common;

import com.google.common.collect.Sets;
import org.qiunet.utils.async.LazyLoader;
import org.qiunet.utils.async.future.DFuture;
import org.qiunet.utils.listener.event.EventHandlerWeightType;
import org.qiunet.utils.listener.event.EventListener;
import org.qiunet.utils.listener.event.data.ServerShutdownEvent;
import org.qiunet.utils.logger.LogUtils;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.string.StringUtil;
import org.qiunet.utils.system.OSUtil;
import org.qiunet.utils.thread.IThreadSafe;
import org.qiunet.utils.thread.ThreadPoolManager;
import org.qiunet.utils.timer.TimerManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/***
 * 销毁时候, 需要一定调用 {@link MessageHandler#destroy()} !!!!!!!!!!!!!
 *
 * @author qiunet
 * 2020-02-08 20:53
 **/
public abstract class MessageHandler<H extends IMessageHandler<H>>
		implements IMessageHandler<H>, IThreadSafe {
	private static final MessageHandlerEventLoop executorService = new MessageHandlerEventLoop(OSUtil.availableProcessors() * 2);

	private final LazyLoader<DExecutorService> executor = new LazyLoader<>(() -> executorService.getEventLoop(this.msgExecuteIndex()));

	private final Logger logger = LoggerType.DUODUO_FLASH_HANDLER.getLogger();

	private final Set<Future<?>> scheduleFutures = Sets.newConcurrentHashSet();

	private final AtomicBoolean destroyed = new AtomicBoolean();

	/**
	 * 执行消息
	 * @param message 消息
	 */
	private void executorMessage(IMessage<H> message) {
		try {
			message.execute((H) this);
		}catch (Exception e) {
			logger.error("Message handler exception:", e);
		}
	}

	/**
	 * 添加一条可以执行消息
	 * @param msg
	 */
	@Override
	public boolean addMessage(IMessage<H> msg) {
		if (this.isDestroyed()) {
			logger.error(LogUtils.dumpStack("MessageHandler ["+getIdentity()+"] 已经关闭销毁"));
			return false;
		}
		executor.get().execute(() -> this.executorMessage(msg));
		return true;
	}

	@Override
	public void runMessage(IMessage<H> message) {
		if (this.isDestroyed()) {
			logger.error(LogUtils.dumpStack("MessageHandler ["+getIdentity()+"] 已经关闭销毁"));
			return;
		}

		ThreadPoolManager.NORMAL.execute(() -> this.executorMessage(message));
	}


	@Override
	public boolean inSelfThread() {
		return executor.get().inSelfThread();
	}

	/**
	 * 一个标识.
	 * 最好能明确的找到问题的id. 比如玩家id什么的.
	 * @return
	 */
	public String getIdentity(){
		return StringUtil.format("({0}:{1})", getClass().getSimpleName(), this.msgExecuteIndex());
	}

	/**
	 * 销毁时候调用
	 */
	@Override
	public void destroy(){
		if (destroyed.compareAndSet(false, true)) {
			this.cancelAllFuture(true);
		}else {
			logger.warn("{} was destroy repeated!", getIdentity());
		}
	}

	/**
	 * 是否已经销毁
	 * @return
	 */
	public boolean isDestroyed() {
		return destroyed.get();
	}
	/**
	 * 结束所有的调度
	 */
	public void cancelAllFuture(boolean mayInterruptIfRunning) {
		scheduleFutures.forEach(scheduleFuture -> scheduleFuture.cancel(mayInterruptIfRunning));
	}
	/***
	 * 循环执行某个调度
	 * @param scheduleName 调度名称
	 * @param msg 消息
	 * @param delay 延迟
	 * @param period 间隔
	 * @return
	 */
	public Future<?> scheduleAtFixedRate(String scheduleName, IMessage<H> msg, long delay, long period, TimeUnit unit) {
		ScheduledFuture<?> future = TimerManager.instance.scheduleAtFixedRate(() -> this.addMessage(msg), delay, period, unit);
		return new ScheduleFuture(scheduleName, future);
	}

	/**
	 * 指定一定的延迟时间后, 执行该消息
	 * @param msg 消息
	 * @param delay 延迟数
	 * @param unit 延迟单位
	 * @return
	 */
	@Override
	public DFuture<Void> scheduleMessage(IMessage<H> msg, long delay, TimeUnit unit) {
		DFuture<Void> future = TimerManager.instance.scheduleWithDelay(() -> {
			addMessage(msg);
			return null;
		}, delay, unit);
		future.whenComplete((res, e) -> this.scheduleFutures.remove(future));
		this.scheduleFutures.add(future);
		return future;
	}
	@EventListener(EventHandlerWeightType.LOWEST)
	private static void shutdown(ServerShutdownEvent event) {
		for (DExecutorService service : executorService.eventLoops) {
			service.shutdown();
		}
	}

	private static class MessageHandlerEventLoop {
		private final List<DExecutorService> eventLoops;

		public MessageHandlerEventLoop(int count) {
			this.eventLoops = IntStream.range(0, count)
				.mapToObj(DExecutorService::new)
			.toList();
		}

		public DExecutorService getEventLoop(Object key) {
			int i = Math.abs(Objects.requireNonNull(key).hashCode()) % eventLoops.size();
			return eventLoops.get(i);
		}
	}

	private static class DExecutorService extends ThreadPoolExecutor implements IThreadSafe {
		private final String threadName;
		private Thread thread;

		public DExecutorService(int id) {
			super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
			this.threadName = "message-handler-"+id;
			this.setThreadFactory(this::newThread);
		}

		private Thread newThread(Runnable runnable) {
			this.thread = new Thread(runnable, this.threadName);
			this.thread.setDaemon(true);
			return this.thread;
		}
		@Override
		public boolean inSelfThread() {
			return this.thread == Thread.currentThread();
		}
	}

	private class ScheduleFuture implements Future<Object> {
		private final String scheduleName;
		private final Future<?> future;

		ScheduleFuture(String scheduleName, Future<?> future) {
			this.scheduleName = scheduleName;
			this.future = future;

			scheduleFutures.add(this);
		}

		@Override
		public String toString() {
			return scheduleName;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			scheduleFutures.remove(this);
			return future.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return  future.isCancelled();
		}

		@Override
		public boolean isDone() {
			return future.isDone();
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return future.get();
		}

		@Override
		public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return future.get(timeout, unit);
		}
	}
}
