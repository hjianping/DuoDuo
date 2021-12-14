package org.qiunet.flash.handler.netty.server.tcp.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.qiunet.flash.handler.common.enums.ServerConnType;
import org.qiunet.flash.handler.common.id.IProtocolId;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.flash.handler.netty.server.constants.CloseCause;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.flash.handler.netty.server.param.TcpBootstrapParams;
import org.qiunet.flash.handler.util.ChannelUtil;
import org.qiunet.utils.logger.LoggerType;
import org.slf4j.Logger;


/**
 * Created by qiunet.
 * 17/8/13
 */
public class TcpServerHandler extends SimpleChannelInboundHandler<MessageContent> {
	private static final Logger logger = LoggerType.DUODUO_FLASH_HANDLER.getLogger();
	private final TcpBootstrapParams params;

	public TcpServerHandler(TcpBootstrapParams params) {
		this.params = params;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().attr(ServerConstants.HANDLER_TYPE_KEY).set(ServerConnType.TCP);
		DSession session = new DSession(ctx.channel());

		ChannelUtil.bindSession(session);
		ctx.channel().attr(ServerConstants.HANDLER_PARAM_KEY).set(params);
		ctx.channel().attr(ServerConstants.MESSAGE_ACTOR_KEY).set(params.getStartupContext().buildMessageActor(session));
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, MessageContent content) throws Exception {

		if (content.getProtocolId() == IProtocolId.System.CLIENT_PING) {
			ctx.writeAndFlush(params.getStartupContext().serverPongMsg().encode());
			return;
		}

		ChannelUtil.channelRead(ctx.channel(), params, content);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		DSession session = ChannelUtil.getSession(ctx.channel());
		String errMeg = "Exception session ["+(session != null ? session.toString(): "null")+"]";
		logger.error(errMeg, cause);

		if (ctx.channel().isOpen() || ctx.channel().isActive()) {
			ctx.writeAndFlush(params.getStartupContext().exception(cause).encode()).addListener(ChannelFutureListener.CLOSE);
			if (session != null) {
				session.close(CloseCause.EXCEPTION);
			}else {
				ctx.close();
			}
		}
	}
}
