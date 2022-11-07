package org.qiunet.flash.handler.netty.server;

import io.netty.util.CharsetUtil;
import org.qiunet.cfg.manager.CfgManagers;
import org.qiunet.flash.handler.common.player.UserOnlineManager;
import org.qiunet.flash.handler.common.player.proto.CrossPlayerLogoutPush;
import org.qiunet.flash.handler.common.player.proto.PlayerReLoginPush;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.flash.handler.netty.server.event.ServerStartupCompleteEvent;
import org.qiunet.flash.handler.netty.server.hook.Hook;
import org.qiunet.flash.handler.netty.server.kcp.NettyKcpServer;
import org.qiunet.flash.handler.netty.server.param.ServerBootStrapParam;
import org.qiunet.flash.handler.netty.server.tcp.NettyTcpServer;
import org.qiunet.utils.classLoader.ClassHotSwap;
import org.qiunet.utils.collection.enums.ForEachResult;
import org.qiunet.utils.exceptions.CustomException;
import org.qiunet.utils.listener.event.data.ServerClosedEvent;
import org.qiunet.utils.listener.event.data.ServerDeprecatedEvent;
import org.qiunet.utils.listener.event.data.ServerShutdownEventData;
import org.qiunet.utils.listener.event.data.ServerStartupEventData;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.net.NetUtil;
import org.qiunet.utils.string.StringUtil;
import org.qiunet.utils.timer.TimerManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 服务器启动类
 *
 * Created by qiunet.
 * 17/11/21
 */
public class BootstrapServer {
	private static final Logger logger = LoggerType.DUODUO_FLASH_HANDLER.getLogger();
	private static BootstrapServer instance;

	private final Set<INettyServer> nettyServers = new HashSet<>(8);
	private final HookListener hookListener;

	private BootstrapServer(Hook hook) {
		if (instance != null) throw new CustomException("Instance Duplication!");
		if (hook == null) {
			throw new CustomException("hook can not be null");
		}
		ServerConstants.startDt.compareAndSet(0, System.currentTimeMillis());
		hookListener = new HookListener(hook);
	}
	/***
	 * 可以自己添加给hook.
	 * @param hook  钩子 关闭时候,先执行你的代码
	 * @return 实例
	 */
	public static BootstrapServer createBootstrap(Hook hook) {
		if (StringUtil.isEmpty(hook.getShutdownMsg())) {
			throw new NullPointerException("shutdownMsg can not be empty!");
		}

		synchronized (BootstrapServer.class) {
			if (instance == null)
			{
				instance = new BootstrapServer(hook);
			}
		}
		return instance;
	}

	/***
	 * 给服务器的钩子发送消息, 需要另起Main线程. 所以无法读取到之前的BootstrapServer .
	 * 默认给本地的端口发送
	 * @param hookPort 钩子端口
	 * @param msg 发送消息内容
	 */
	public static void sendHookMsg(int hookPort, String msg) {
		if (hookPort <= 0) {
			logger.error("BootstrapServer sendHookMsg but hookPort is less than 0!");
			System.exit(1);
		}
		logger.error("BootstrapServer sendHookMsg [{}]!", msg);
		try {
			NetUtil.udpSendData("localhost", hookPort, msg.getBytes(CharsetUtil.UTF_8));
		} catch (IOException e) {
			logger.error("BootstrapServer sendHookMsg: ", e);
			System.exit(1);
		}
	}

	/**
	 * 监听一个服务
	 * 服务可以是kcp tcp  http  ws 会自动切换
	 * @param param server的参数
	 * @return  BootstrapServer 实例
	 */
	public BootstrapServer listener(ServerBootStrapParam param) {
		// 默认启动tcp  http监听
		this.nettyServers.add(new NettyTcpServer(param));
		if (param.getKcpParam() != null) {
			this.nettyServers.add(new NettyKcpServer(param));
		}
		return this;
	}
	private Thread awaitThread;
	/***
	 * 阻塞线程 最后调用阻塞当前线程
	 */
	public void await(){
		this.await(null);
	}
	public void await(Runnable completeRunnable){
		Thread hookThread = new Thread(hookListener, "HookListener");
		hookThread.setDaemon(true);
		hookThread.start();
		try {
			ServerStartupEventData.fireStartupEventHandler();
		}catch (CustomException e) {
			e.logger(logger);
			System.exit(1);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		}

		for (INettyServer nettyServer : nettyServers) {
			Thread thread = new Thread(nettyServer, nettyServer.threadName());
			thread.setDaemon(true);
			thread.start();
		}
		try {
			// 等nettyServer的启动.
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		ServerStartupCompleteEvent.fireStartupCompleteEvent();
		awaitThread = Thread.currentThread();
		if (completeRunnable != null) {
			completeRunnable.run();
		}
		LockSupport.park();
	}
	/***
	 * Hook的监听
	 */
	private final class HookListener implements Runnable {
		private final ByteBuffer buffer = ByteBuffer.allocate(256);
		private final AtomicBoolean deprecated = new AtomicBoolean();
		private final AtomicBoolean shutdown = new AtomicBoolean();
		private final AtomicBoolean serverClose = new AtomicBoolean();
		private final Hook hook;
		HookListener(Hook hook) {
			Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
			this.hook = hook;
		}

		/**
		 * 重新加载配置
		 */
		private void reloadCfg(){
			try {
				CfgManagers.getInstance().reloadSetting();
			} catch (Throwable throwable) {
				LoggerType.DUODUO.error("Exception: ", throwable);
			}
		}
		/**
		 * 通过shutdown 监听. 停止服务
		 */
		private void shutdown(){
			if (! shutdown.compareAndSet(false, true)) {
				return;
			}
			// 停止玩家进入 请求
			ServerClosedEvent.fireClosed();
			// 触发停服事件
			ServerShutdownEventData.fireShutdownEventHandler();

			// 停止所有服务
			for (INettyServer server : nettyServers) {
				server.shutdown();
			}

			// 停止共用的线程池
			ServerConstants.WORKER.shutdownGracefully();
			// 放开主线程
			LockSupport.unpark(awaitThread);
		}

		/**
		 * 对外服务关闭
		 */
		private void serverClose() {
			if (! serverClose.compareAndSet(false, true)) {
				return;
			}
			ServerClosedEvent.fireClosed();
		}

		/**
		 * 让服务器过期
		 */
		private void deprecated(){
			if (! deprecated.compareAndSet(false, true)) {
				return;
			}

			ServerDeprecatedEvent.fireDeprecated();

			UserOnlineManager.instance.foreach(actor -> {
				logger.debug("Push message to online user {}", actor.getId());
				if (actor.isPlayerActor()) {
					actor.sendMessage(PlayerReLoginPush.valueOf());
				}else if (actor.isCrossPlayer()) {
					// 担心有的房间很长时间. 如果客户端有办法. 就退出. 没有. 该服务器也不再分配出去.
					actor.sendMessage(CrossPlayerLogoutPush.instance);
				}
				return ForEachResult.CONTINUE;
			});

			if (UserOnlineManager.instance.onlineSize() <= 0) {
				this.shutdown();
				return;
			}
			this.waitForPlayerClean();
		}

		/**
		 * 等待服务器玩家清零
		 */
		private void waitForPlayerClean() {
			TimerManager.instance.scheduleWithDelay(() -> {
				if (UserOnlineManager.instance.onlineSize() > 0) {
					this.waitForPlayerClean();
					return;
				}
				this.shutdown();
			}, 2, TimeUnit.SECONDS);
		}

		/***
		 * 处理现有的消息. 可以用户自定义
		 * @param channel channel
		 */
		private void handlerMsg(DatagramChannel channel) {
			buffer.clear();
			try {
				channel.receive(buffer);
			} catch (IOException e) {
				logger.error("handlerMsg", e);
			}
			buffer.flip();
			String msg = CharsetUtil.UTF_8.decode(buffer).toString();
			msg = StringUtil.powerfulTrim(msg);
			logger.error("[HookListener]服务端 Received Msg: [{}]", msg);
			if (msg.equalsIgnoreCase(hook.getShutdownMsg())) {
				this.shutdown();
			}else if (msg.equalsIgnoreCase(hook.getReloadCfgMsg())){
				this.reloadCfg();
			}else if (msg.equalsIgnoreCase(hook.getDeprecateMsg())) {
				this.deprecated();
			}else if (msg.equalsIgnoreCase(hook.getServerCloseMsg())) {
				this.serverClose();
			} else if (msg.equalsIgnoreCase(hook.hotswapMsg())) {
				ClassHotSwap.hotSwap(Paths.get(System.getProperty("hotSwap.dir")));
			}else {
				hook.custom(msg);
			}
		}

		@Override
		public void run() {
			logger.error("[HookListener]服务端 Hook Listener on udp port [{}]", hook.getHookPort());
			try(DatagramChannel channel = DatagramChannel.open()) {
				try {
					channel.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), this.hook.getHookPort()));
				}catch (Exception e) {
					logger.error("Bind error", e);
					System.exit(1);
				}

				while (! shutdown.get()) {
					this.handlerMsg(channel);
				}
			} catch (IOException e) {
				logger.error("[HookListener]", e);
			}
		}
	}
}
