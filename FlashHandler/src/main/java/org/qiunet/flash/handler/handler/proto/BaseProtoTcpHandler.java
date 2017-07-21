package org.qiunet.flash.handler.handler.proto;

import org.qiunet.flash.handler.handler.BaseTcpHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by qiunet.
 * 17/7/21
 */
public abstract class BaseProtoTcpHandler<RequestData> extends BaseTcpHandler<RequestData> {
	@Override
	public RequestData parseRequestData(byte[] bytes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method method = requestDataClass.getMethod("parseFrom", byte[].class);
		return (RequestData) method.invoke(null, bytes);
	}
}
