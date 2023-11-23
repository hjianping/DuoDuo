package org.qiunet.flash.handler.context.request;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.context.request.data.ChannelDataMapping;
import org.qiunet.flash.handler.context.session.ISession;
import org.qiunet.flash.handler.handler.IHandler;
import org.qiunet.flash.handler.handler.mapping.UrlRequestHandlerMapping;
import org.qiunet.utils.logger.LoggerType;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Created by qiunet.
 * 17/11/20
 */
public abstract class BaseRequestContext<RequestData> implements IRequestContext<RequestData> {
	protected Logger logger = LoggerType.DUODUO_FLASH_HANDLER.getLogger();

	protected IHandler<RequestData> handler;
	protected Map<String , Object> attributes;
	// 请求数据. 必须在这里获取. 然后release MessageContent
	protected RequestData requestData;

	protected ISession session;

	protected void init(ISession session, MessageContent content) {
		this.session = Preconditions.checkNotNull(session);
		if (content.getProtocolId() > 0) {
			this.handler = Preconditions.checkNotNull(ChannelDataMapping.getHandler(content.getProtocolId()));
		}else {
			this.handler = Preconditions.checkNotNull(UrlRequestHandlerMapping.getHandler(content.getUriPath()));
		}
		this.requestData = getHandler().parseRequestData(content.byteBuffer());
	}

	public ISession getSession() {
		return session;
	}

	@Override
	public RequestData getRequestData() {
		return requestData;
	}

	@Override
	public IHandler<RequestData> getHandler() {
		return handler;
	}

	@Override
	public Object getAttribute(String key) {
		return attributes == null ? null : attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object val) {
		if (attributes == null) {
			attributes = Maps.newConcurrentMap();
		}
		attributes.put(key, val);
	}
}
