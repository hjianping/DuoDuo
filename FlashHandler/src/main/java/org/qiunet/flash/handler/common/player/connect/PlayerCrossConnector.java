package org.qiunet.flash.handler.common.player.connect;

import org.qiunet.cross.actor.auth.CrossPlayerAuthRequest;
import org.qiunet.cross.common.trigger.PlayerConnectorClientTrigger;
import org.qiunet.cross.event.CrossEventRequest;
import org.qiunet.cross.node.ServerInfo;
import org.qiunet.cross.node.ServerNodeManager;
import org.qiunet.flash.handler.common.player.PlayerActor;
import org.qiunet.flash.handler.common.player.event.UserEventData;
import org.qiunet.flash.handler.context.sender.IChannelMessageSender;
import org.qiunet.flash.handler.context.session.ISession;
import org.qiunet.flash.handler.netty.client.param.TcpClientParams;
import org.qiunet.flash.handler.netty.client.tcp.NettyTcpClient;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.flash.handler.netty.server.message.ConnectionReq;
import org.qiunet.flash.handler.netty.server.param.adapter.message.ClientPingRequest;
import org.qiunet.utils.exceptions.CustomException;

/***
 *  tcp 连接
 *
 * @author qiunet
 * 2021/11/16 17:32
 */
public class PlayerCrossConnector implements IChannelMessageSender {
	/**
	 * client
	 */
	private static final NettyTcpClient tcpClient = NettyTcpClient.create(TcpClientParams.custom().build(), new PlayerConnectorClientTrigger());
	/**
	 * session
	 */
	private final ISession session;
	/**
	 *
	 */
	private final long playerId;
	/**
	 * serverId
	 */
	private final int serverId;
	/**
	 * 使用actor 和 serverId 构造一个connector
	 * @param actor
	 * @return
	 */
	public PlayerCrossConnector(PlayerActor actor, int serverId) {
		if (serverId == ServerNodeManager.getCurrServerId()) {
			throw new CustomException("connect to self!");
		}
		ServerInfo serverInfo = ServerNodeManager.getServerInfo(serverId);
		// 因为大部分服务都是内网组网. 所以使用host.如果以后不在内网. 有两个解决方案
		// 1. 直接修改下面为publicHost . 2. 云运营商跨区域组网
		this.session = tcpClient.connect(serverInfo.getHost(), serverInfo.getCrossPort(), f -> f.channel().attr(ServerConstants.MESSAGE_ACTOR_KEY).set(actor));
		this.session.addCloseListener("connectorQuitCross", ((session1, cause) -> actor.quitCross(serverInfo.getServerType(), cause)));
		session.sendMessage(ConnectionReq.valueOf(String.valueOf(actor.getId())), true);

		session.sendMessage(CrossPlayerAuthRequest.valueOf(actor.getId(), ServerNodeManager.getCurrServerId(), actor.isKcpSessionPrepare()), true);
		this.playerId = actor.getPlayerId();
		this.serverId = serverId;
	}

	public int getServerId() {
		return serverId;
	}

	/**
	 * 心跳
	 */
	public void heartBeat(){
		this.sendMessage(ClientPingRequest.valueOf());
	}

	/**
	 * 获得session
	 * @return
	 */
	public ISession getSession() {
		return session;
	}

	/**
	 * 触发该服务的事件
	 * @param event
	 * @param <Event>
	 */
	public <Event extends UserEventData> void fireCrossEvent(Event event) {
		CrossEventRequest request = CrossEventRequest.valueOf(event);
		session.sendMessage(request.buildChannelMessage());
	}

	@Override
	public IChannelMessageSender getSender() {
		return session;
	}
}
