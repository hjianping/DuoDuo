package org.qiunet.flash.handler.handler.websocket;


import org.qiunet.flash.handler.common.player.IMessageActor;
import org.qiunet.flash.handler.context.request.websocket.IWebSocketRequest;
import org.qiunet.flash.handler.handler.IHandler;

/**
 * @author qiunet
 *         Created on 17/3/3 12:01.
 */
public interface IWebSocketHandler<P extends IMessageActor, RequestData> extends IHandler<RequestData> {
	/**
	 * tcp udp 处理. 下行一般在逻辑里面处理了
	 * @param context
	 * @return
	 */
	void handler(P playerActor, IWebSocketRequest<RequestData> context)throws Exception;
}
