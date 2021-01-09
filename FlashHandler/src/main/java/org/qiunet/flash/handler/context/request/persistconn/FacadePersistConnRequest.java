package org.qiunet.flash.handler.context.request.persistconn;


import io.netty.channel.Channel;
import org.qiunet.flash.handler.common.player.IMessageActor;

/**
 * Created by qiunet.
 * 17/12/2
 */
class FacadePersistConnRequest<RequestData, P extends IMessageActor> implements IPersistConnRequest<RequestData> {
	private AbstractPersistConnRequestContext<RequestData, P> context;
	public FacadePersistConnRequest(AbstractPersistConnRequestContext<RequestData, P> context) {
		this.context = context;
	}

	@Override
	public RequestData getRequestData() {
		return context.getRequestData();
	}

	@Override
	public String getRemoteAddress() {
		return context.getRemoteAddress();
	}

	@Override
	public Object getAttribute(String key) {
		return context.getAttribute(key);
	}

	@Override
	public void setAttribute(String key, Object val) {
		context.setAttribute(key, val);
	}

	@Override
	public Channel channel() {
		return context.channel();
	}
}
