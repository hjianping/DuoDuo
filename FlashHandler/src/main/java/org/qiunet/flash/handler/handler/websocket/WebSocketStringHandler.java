package org.qiunet.flash.handler.handler.websocket;

import org.qiunet.flash.handler.common.enums.DataType;
import org.qiunet.flash.handler.common.player.IPlayerActor;

import java.nio.charset.StandardCharsets;

/**
 * Created by qiunet.
 * 17/11/21
 */
public abstract class WebSocketStringHandler<P extends IPlayerActor> extends BaseWebSocketHandler<P, String> {

	@Override
	public DataType getDataType() {
		return DataType.STRING;
	}

	@Override
	public String parseRequestData(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
