package org.qiunet.cross.actor.auth;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import org.qiunet.flash.handler.common.id.IProtocolId;
import org.qiunet.flash.handler.context.request.data.ChannelData;
import org.qiunet.flash.handler.context.request.data.IChannelData;
import org.qiunet.flash.handler.util.SkipProtoGenerator;

/***
 *
 *
 * @author qiunet
 * 2020-10-23 16:50
 */
@SkipProtoGenerator
@ChannelData(ID = IProtocolId.System.CROSS_PLAYER_AUTH, desc = "跨服鉴权")
public class CrossPlayerAuthRequest implements IChannelData {
	@Protobuf(description = "玩家id")
	private long playerId;
	@Protobuf(description = "玩家的serverId")
	private int serverId;

	public static CrossPlayerAuthRequest valueOf(long playerId, int serverId) {
		CrossPlayerAuthRequest request = new CrossPlayerAuthRequest();
		request.playerId = playerId;
		request.serverId = serverId;
		return request;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}
}
