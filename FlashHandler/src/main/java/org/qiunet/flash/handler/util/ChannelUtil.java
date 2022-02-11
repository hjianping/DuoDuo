package org.qiunet.flash.handler.util;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.Attribute;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.common.player.ICrossStatusActor;
import org.qiunet.flash.handler.common.player.IMessageActor;
import org.qiunet.flash.handler.context.header.IProtocolHeader;
import org.qiunet.flash.handler.context.header.IProtocolHeaderType;
import org.qiunet.flash.handler.context.request.data.ChannelDataMapping;
import org.qiunet.flash.handler.context.request.persistconn.IPersistConnRequestContext;
import org.qiunet.flash.handler.context.response.push.IChannelMessage;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.flash.handler.handler.IHandler;
import org.qiunet.flash.handler.netty.server.constants.CloseCause;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.flash.handler.netty.server.param.AbstractBootstrapParam;
import org.qiunet.flash.handler.netty.transmit.ITransmitHandler;
import org.qiunet.flash.handler.netty.transmit.TransmitRequest;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.string.StringUtil;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Arrays;

public final class ChannelUtil {
	private static final Logger logger = LoggerType.DUODUO_FLASH_HANDLER.getLogger();
	private ChannelUtil(){}
	/***
	 * 得到channel保存的ProtocolHeader数据
	 * @param channel
	 * @return
	 */
	public static IProtocolHeaderType getProtocolHeaderAdapter(Channel channel) {
		return channel.attr(ServerConstants.PROTOCOL_HEADER_ADAPTER).get();
	}

	/***
	 * 将一个MessageContent 转为 有 Header 的 ByteBuf
	 * @param message
	 * @param channel
	 * @return
	 */
	public static ByteBuf messageContentToByteBuf(IChannelMessage<?> message, Channel channel) {
		byte[] bytes = message.bytes();

		IProtocolHeaderType adapter = getProtocolHeaderAdapter(channel);
		IProtocolHeader header = adapter.outHeader(message.getProtocolID(), bytes);
		ByteBuf byteBuf = Unpooled.wrappedBuffer(header.dataBytes(), bytes);

		if (LoggerType.DUODUO_FLASH_HANDLER.isDebugEnabled()) {
			LoggerType.DUODUO_FLASH_HANDLER.debug("header: {}", Arrays.toString(header.dataBytes()));
			LoggerType.DUODUO_FLASH_HANDLER.debug("body: {}", Arrays.toString(bytes));
		}
		return byteBuf;
	}

	/***
	 * 将一个MessageContent 转为 有 Header 的 ByteBuf
	 * @param message
	 * @return
	 */
	public static ByteBuf messageContentToByteBufWithoutHeader(IChannelMessage<?> message) {
		byte[] bytes = message.bytes();
		ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
		if (LoggerType.DUODUO_FLASH_HANDLER.isDebugEnabled()) {
			LoggerType.DUODUO_FLASH_HANDLER.debug("body: {}", Arrays.toString(bytes));
		}
		return byteBuf;
	}

	/***
	 * 关联Session 和 channel
	 * @param val
	 * @return
	 */
	public static boolean bindSession(DSession val) {
		Preconditions.checkNotNull(val);
		Attribute<DSession> attr = val.channel().attr(ServerConstants.SESSION_KEY);
		boolean result = attr.compareAndSet(null, val);
		if (! result) {
			logger.error("Session [{}] Duplicate", val);
			val.close(CloseCause.LOGIN_REPEATED);
		}
		return result;
	}

	/***
	 * 得到一个Session
	 * @param channel
	 * @return
	 */
	public static DSession getSession(Channel channel) {
		return channel.attr(ServerConstants.SESSION_KEY).get();
	}

	/**
	 *  获得ip
	 * @return
	 */
	public static String getIp(Channel channel) {
		return getIp(channel.attr(ServerConstants.HTTP_WS_HEADER_KEY).get(), channel);
	}

	/**
	 * 得到真实ip. http类型的父类
	 * @param headers
	 * @return
	 */
	public static String getIp(HttpHeaders headers, Channel channel) {
		String ip;
		if (headers != null) {
			if (!StringUtil.isEmpty(ip = headers.get("x-forwarded-for")) && !"unknown".equalsIgnoreCase(ip)) {
				return ip;
			}

			if (! StringUtil.isEmpty(ip = headers.get("HTTP_X_FORWARDED_FOR")) && ! "unknown".equalsIgnoreCase(ip)) {
				return ip;
			}

			if (!StringUtil.isEmpty(ip = headers.get("x-forwarded-for-pound")) &&! "unknown".equalsIgnoreCase(ip)) {
				return ip;
			}

			if (!StringUtil.isEmpty(ip = headers.get("Proxy-Client-IP") ) &&! "unknown".equalsIgnoreCase(ip)) {
				return ip;
			}

			if (!StringUtil.isEmpty(ip = headers.get("WL-Proxy-Client-IP")) &&! "unknown".equalsIgnoreCase(ip)) {
				return ip;
			}
		}
		if (channel.remoteAddress() == null) {
			return "unknown-address";
		}

		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

	/**
	 * 处理长连接的通道读数据
	 * @param channel
	 * @param params
	 * @param content
	 */
	public static void channelRead(Channel channel, AbstractBootstrapParam params, MessageContent content){
		DSession session = ChannelUtil.getSession(channel);
		Preconditions.checkNotNull(session);

		if (! params.getStartupContext().userServerValidate(session)) {
			return;
		}

		IHandler handler = ChannelDataMapping.getHandler(content.getProtocolId());
		if (handler == null) {
			channel.writeAndFlush(params.getStartupContext().getHandlerNotFound());
			content.release();
			return;
		}

		IMessageActor messageActor = session.getAttachObj(ServerConstants.MESSAGE_ACTOR_KEY);
		if (handler instanceof ITransmitHandler
				&& messageActor instanceof ICrossStatusActor
				&& ((ICrossStatusActor) messageActor).isCrossStatus()) {
			((ICrossStatusActor) messageActor).sendCrossMessage(TransmitRequest.valueOf(content.getProtocolId(), content.bytes()));
			return;
		}
		if (channel.isActive()) {
			IPersistConnRequestContext context = handler.getDataType().createPersistConnRequestContext(content, channel, handler, messageActor);
			messageActor.addMessage(context);
		}else{
			content.release();
		}
	}
}
