package org.qiunet.flash.handler.handler.websocket;

import com.google.protobuf.GeneratedMessageV3;
import org.qiunet.flash.handler.common.enums.DataType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by qiunet.
 * 17/7/21
 */
public abstract class WebSocketProtobufHandler<RequestData extends GeneratedMessageV3> extends BaseWebSocketHandler<RequestData> {
	private Method method;
	@Override
	public RequestData parseRequestData(byte[] bytes) {
		try {
			if (method == null ) method = getRequestClass().getMethod("parseFrom", byte[].class);
			return (RequestData) method.invoke(null, bytes);
		} catch (Exception e) {
			logger.error("["+getClass().getSimpleName()+"] Exception: ", e);
		}
		return null;
	}

	@Override
	public DataType getDataType() {
		return DataType.PROTOBUF;
	}
}
