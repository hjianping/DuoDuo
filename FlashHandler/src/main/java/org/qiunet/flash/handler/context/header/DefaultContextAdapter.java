package org.qiunet.flash.handler.context.header;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.qiunet.flash.handler.context.request.http.HttpProtobufRequestContext;
import org.qiunet.flash.handler.context.request.http.HttpStringRequestContext;
import org.qiunet.flash.handler.context.request.http.IHttpRequestContext;
import org.qiunet.flash.handler.context.request.tcp.ITcpRequestContext;
import org.qiunet.flash.handler.context.request.tcp.TcpProtobufRequestContext;
import org.qiunet.flash.handler.context.request.tcp.TcpStringRequestContext;
import org.qiunet.flash.handler.handler.IHandler;
import org.qiunet.flash.handler.handler.mapping.RequestHandlerMapping;

/**
 * Created by qiunet.
 * 17/11/22
 */
public class DefaultContextAdapter implements ContextAdapter {

	@Override
	public IHandler getHandler(MessageContent content) {
		if (content == null) return null;

		if (content.getProtocolId() > 0) {
			return RequestHandlerMapping.getInstance().getGameHandler(content.getProtocolId());
		}else {
			return RequestHandlerMapping.getInstance().getOtherRequestHandler(content.getUriPath());
		}
	}

	@Override
	public IHttpRequestContext createHttpRequestContext(MessageContent content, ChannelHandlerContext channelContext, IHandler handler, HttpRequest request) {
		switch (handler.getDataType()) {
			case STRING:
				return new HttpStringRequestContext(content, channelContext, request);
			case PROTOBUF:
				return new HttpProtobufRequestContext(content, channelContext, request);
		}
		return null;
	}

	@Override
	public ITcpRequestContext createTcpRequestContext(MessageContent content, ChannelHandlerContext channelContext, IHandler handler) {
		switch (handler.getDataType()) {
			case STRING:
				return new TcpStringRequestContext(content, channelContext);
			case PROTOBUF:
				return new TcpProtobufRequestContext(content, channelContext);
		}
		return null;
	}
}
