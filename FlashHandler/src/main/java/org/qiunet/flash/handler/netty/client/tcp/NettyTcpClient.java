package org.qiunet.flash.handler.netty.client.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.qiunet.flash.handler.common.enums.ServerConnType;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.flash.handler.context.session.ISession;
import org.qiunet.flash.handler.context.session.config.DSessionConnectParam;
import org.qiunet.flash.handler.netty.client.param.TcpClientConfig;
import org.qiunet.flash.handler.netty.client.trigger.IPersistConnResponseTrigger;
import org.qiunet.flash.handler.netty.coder.TcpSocketClientDecoder;
import org.qiunet.flash.handler.netty.coder.TcpSocketClientEncoder;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.flash.handler.util.NettyUtil;
import org.qiunet.utils.logger.LoggerType;

/**
 * Created by qiunet.
 * 17/11/25
 */
public class NettyTcpClient {
	private static final EventLoopGroup group = NettyUtil.newEventLoopGroup(8, "netty-tcp-client-event-loop-");
	private final IPersistConnResponseTrigger trigger;
	private final TcpClientConfig config;
	private final Bootstrap bootstrap;
	/**
	 *
	 * @param config
	 */
	private NettyTcpClient(TcpClientConfig config, IPersistConnResponseTrigger trigger) {
		this.bootstrap = new Bootstrap();
		Class<? extends SocketChannel> socketChannelClz = Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
		this.bootstrap.option(ChannelOption.TCP_NODELAY,true);
		this.bootstrap.handler(new NettyClientInitializer());
		this.bootstrap.channel(socketChannelClz);
		this.bootstrap.group(group);
		this.trigger = trigger;
		this.config = config;
	}

	/**
	 * 阻塞 直到连接成功后返回.
	 * @param config
	 * @return
	 */
	public static NettyTcpClient create(TcpClientConfig config, IPersistConnResponseTrigger trigger) {
		return new NettyTcpClient(config, trigger);
	}

	/**
	 * 连接服务器.
	 * @param host
	 * @param port
	 * @return
	 */
	public ISession connect(String host, int port, GenericFutureListener<ChannelFuture> listener) {
		return new DSession(
				DSessionConnectParam.newBuilder(() -> bootstrap.connect(host, port))
				.setConnectListener(listener)
				.build()
		);
	}

	public TcpClientConnector connect(String host, int port) {
		return new TcpClientConnector(connect(host, port, null));
	}

	public static void shutdown(){
		if (! group.isShutdown()) group.shutdownGracefully();
	}

	private class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			ch.attr(ServerConstants.PROTOCOL_HEADER).set(config.getProtocolHeader());
			ch.attr(ServerConstants.HANDLER_TYPE_KEY).set(ServerConnType.TCP);
			pipeline.addLast("TcpSocketEncoder", new TcpSocketClientEncoder());
			pipeline.addLast("TcpSocketDecoder", new TcpSocketClientDecoder(config.getMaxReceivedLength(), config.isEncryption()));
			pipeline.addLast(new NettyClientHandler());
		}
	}

	private class NettyClientHandler extends SimpleChannelInboundHandler<MessageContent> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ctx.channel().attr(ServerConstants.HANDLER_TYPE_KEY).set(ServerConnType.TCP);
			super.channelActive(ctx);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, MessageContent msg) throws Exception {
			trigger.response(ctx.channel().attr(ServerConstants.SESSION_KEY).get(), msg);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			LoggerType.DUODUO_FLASH_HANDLER.error("Netty tcp client exception: ", cause);
		}
	}
}
