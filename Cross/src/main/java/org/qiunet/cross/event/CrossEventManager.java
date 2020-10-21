package org.qiunet.cross.event;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.google.common.base.Preconditions;
import org.qiunet.flash.handler.common.player.AbstractUserActor;
import org.qiunet.flash.handler.common.player.UserOnlineManager;
import org.qiunet.flash.handler.common.player.event.BasePlayerEventData;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.utils.protobuf.ProtobufDataManager;

/***
 * 跨服事件处理
 *
 * @author qiunet
 * 2020-10-14 17:28
 */
public class CrossEventManager {
	/**
	 * 跨服的事件.
	 * 功能服 -> 跨服
	 * 跨服 -> 功能服
	 * @param playerId
	 * @param crossSession
	 * @param eventData
	 */
	public static  <T extends BasePlayerEventData> void fireCrossEvnet(long playerId, DSession crossSession, T eventData) {
		// 当前服的playerActor
		AbstractUserActor playerActor = UserOnlineManager.getPlayerActor(playerId);

		Preconditions.checkState(playerActor != null && playerActor.isCrossStatus(), "player actor must be cross status");
		Preconditions.checkState(eventData.getClass().isAnnotationPresent(ProtobufClass.class), "Class [%s] need specify annotation @ProtobufClass", eventData.getClass().getName());

		byte[] bytes = ProtobufDataManager.encode((Class<T>)eventData.getClass(), eventData);
		CrossEventRequest request = CrossEventRequest.valueOf(eventData.getClass().getName(), bytes);
		crossSession.writeMessage(request.buildResponseMessage());
	}
}
