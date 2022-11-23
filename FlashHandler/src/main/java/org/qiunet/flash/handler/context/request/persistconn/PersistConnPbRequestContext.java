package org.qiunet.flash.handler.context.request.persistconn;

import io.netty.channel.Channel;
import org.qiunet.cross.actor.CrossPlayerActor;
import org.qiunet.flash.handler.common.annotation.SkipDebugOut;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.common.player.IMessageActor;
import org.qiunet.flash.handler.context.request.data.ChannelDataMapping;
import org.qiunet.flash.handler.context.request.data.IChannelData;
import org.qiunet.flash.handler.context.status.StatusResultException;
import org.qiunet.flash.handler.handler.persistconn.IPersistConnHandler;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.flash.handler.netty.transmit.ITransmitHandler;
import org.qiunet.utils.pool.ObjectPool;
import org.qiunet.utils.string.ToString;

/**
 * 该对象会回收. 所以只能在本线程用. addMessage 后. 就会回收掉
 *
 * Created by qiunet.
 * 17/12/2
 */
public class PersistConnPbRequestContext<RequestData extends IChannelData, P extends IMessageActor<P>>
		extends AbstractPersistConnRequestContext<RequestData, P> {

	private static final ObjectPool<PersistConnPbRequestContext> RECYCLER = new ObjectPool<PersistConnPbRequestContext>() {
		@Override
		public PersistConnPbRequestContext newObject(Handle<PersistConnPbRequestContext> handler) {
			return new PersistConnPbRequestContext(handler);
		}
	};

	private final ObjectPool.Handle<PersistConnPbRequestContext> recyclerHandle;

	public PersistConnPbRequestContext(ObjectPool.Handle<PersistConnPbRequestContext> recyclerHandle) {
		this.recyclerHandle = recyclerHandle;
	}

	public static PersistConnPbRequestContext valueOf(MessageContent content, Channel channel, IMessageActor messageActor) {
		PersistConnPbRequestContext context = RECYCLER.get();
		context.init(content, channel, messageActor);
		return context;
	}

	public void init(MessageContent content, Channel channel, P messageActor) {
		super.init(content, channel, messageActor);
	}

	private void recycle() {
		this.messageActor = null;
		this.requestData = null;
		this.attributes = null;
		this.handler = null;
		this.channel = null;

		this.recyclerHandle.recycle();
	}

	@Override
	public void execute(P p) {
		try {
			this.handlerRequest();
		}catch (Exception e) {
			if (! (e instanceof StatusResultException)) {
				logger.error("Execute exception: " , e);
			}
			channel.attr(ServerConstants.BOOTSTRAP_CONFIG_KEY).get().getStartupContext().exception(channel, e);
		} finally {
			this.recycle();
		}
	}

	@Override
	public void handlerRequest() throws Exception{
		if (getRequestData() == null) {
			logger.error("RequestData is null for case playerId {} , protocol: {}", messageActor.getIdentity(), getHandler().getClass().getSimpleName());
			return;
		}
		ChannelDataMapping.requestCheck(channel, getRequestData());

		if (handler.needAuth() && ! messageActor.isAuth()) {
			logger.error("Handler [{}] need auth. but session {} not auth!", handler.getClass().getSimpleName(), messageActor.getSender());
			// 先不管. 客户端重连可能有问题. 不能掐掉
			//ChannelUtil.getSession(channel).close(CloseCause.ERR_REQUEST);
			return;
		}
		long startTime = System.currentTimeMillis();
		if (logger.isInfoEnabled() && ! getRequestData().getClass().isAnnotationPresent(SkipDebugOut.class)) {
			logger.info("[{}] [{}({})] <<< {}", messageActor.getIdentity(), channel().attr(ServerConstants.HANDLER_TYPE_KEY).get(), channel.id().asShortText(), ToString.toString(getRequestData()));
		}

		if (messageActor instanceof CrossPlayerActor && getHandler() instanceof ITransmitHandler) {
			((ITransmitHandler) getHandler()).crossHandler(((CrossPlayerActor) messageActor), getRequestData());
		}else {
			FacadePersistConnRequest<RequestData, P> facadeWebSocketRequest = FacadePersistConnRequest.valueOf(this);
			try {
				((IPersistConnHandler) getHandler()).handler(messageActor, facadeWebSocketRequest);
			}finally {
				facadeWebSocketRequest.recycle();
			}
		}
		long useTime = System.currentTimeMillis() - startTime;
		this.getHandler().recordUseTime(useTime);
	}
}
