package org.qiunet.test.server.error;

import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.context.response.push.IMessage;
import org.qiunet.flash.handler.netty.server.tcp.error.IClientErrorMessage;

/**
 * Created by qiunet.
 * 17/12/11
 */
public class ErrorHandler implements IClientErrorMessage {
	@Override
	public IMessage getHandlerNotFound() {

		return null;
	}

	@Override
	public IMessage exception(Throwable cause) {
		return null;
	}


}
