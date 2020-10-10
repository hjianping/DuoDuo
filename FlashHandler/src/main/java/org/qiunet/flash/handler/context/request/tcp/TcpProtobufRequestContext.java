package org.qiunet.flash.handler.context.request.tcp;

import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.qiunet.flash.handler.common.annotation.SkipDebugOut;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.common.player.IMessageActor;
import org.qiunet.flash.handler.handler.tcp.ITcpHandler;
import org.qiunet.utils.async.LazyLoader;

/**
 * Created by qiunet.
 * 17/11/21
 */
public class TcpProtobufRequestContext<RequestData, P extends IMessageActor> extends AbstractTcpRequestContext<RequestData, P> {
	private LazyLoader<RequestData> requestData = new LazyLoader<>(() -> getHandler().parseRequestData(messageContent.bytes()));

	public TcpProtobufRequestContext(MessageContent content, ChannelHandlerContext channelContext, P messageActor) {
		super(content, channelContext, messageActor);
	}

	@Override
	public RequestData getRequestData() {
		return requestData.get();
	}

	@Override
	public void execute(P p) {
		this.handlerRequest();
	}

	@Override
	public void handlerRequest() {
		FacadeTcpRequest<RequestData, P> facadeTcpRequest = new FacadeTcpRequest<>(this);
		if (logger.isInfoEnabled() && ! getHandler().getClass().isAnnotationPresent(SkipDebugOut.class)) {
			logger.info("[{}] <<< {}", messageActor.getId(), ToStringBuilder.reflectionToString(getRequestData(), ToStringStyle.SHORT_PREFIX_STYLE));
		}

		try {
			((ITcpHandler) getHandler()).handler(messageActor, facadeTcpRequest);
		} catch (Exception e) {
			logger.error("TcpProtobufRequestContext Exception:", e);
		}
	}
}
