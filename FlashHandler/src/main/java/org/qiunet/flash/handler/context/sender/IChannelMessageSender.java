package org.qiunet.flash.handler.context.sender;

import org.qiunet.flash.handler.context.request.data.IChannelData;
import org.qiunet.flash.handler.context.response.push.IChannelMessage;
import org.qiunet.flash.handler.context.session.future.IDSessionFuture;

/***
 * 所有发送响应的都实现该接口
 * @author qiunet
 * 2020/3/1 20:53
 **/
public interface IChannelMessageSender {
	/***
	 * 得到session
	 */
	IChannelMessageSender getSender();

	/**
	 * 发送消息
	 * @param message
	 * @return
	 */
	default IDSessionFuture sendMessage(IChannelMessage<?> message) {
		return getSender().sendMessage(message);
	}

	/**
	 * 发送消息
	 * @param message
	 */
	default IDSessionFuture sendMessage(IChannelData message) {
		return this.sendMessage(message.buildChannelMessage());
	}
	/**
	 * 发送消息
	 * @param message
	 */
	default IDSessionFuture sendMessage(IChannelData message, boolean flush) {
		return this.sendMessage(message.buildChannelMessage(), flush);
	}
	/**
	 * 发送消息
	 * @param message
	 */
	default IDSessionFuture sendMessage(IChannelMessage<?> message, boolean flush){
		return getSender().sendMessage(message, flush);
	}
}
