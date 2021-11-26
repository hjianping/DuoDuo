package org.qiunet.flash.handler.common.player.connect;

import org.qiunet.cross.actor.auth.CrossPlayerAuthRequest;
import org.qiunet.cross.common.trigger.TcpNodeClientTrigger;
import org.qiunet.cross.node.ServerInfo;
import org.qiunet.cross.node.ServerNodeManager;
import org.qiunet.flash.handler.common.player.PlayerActor;
import org.qiunet.flash.handler.context.header.ProtocolHeaderType;
import org.qiunet.flash.handler.context.sender.IChannelMessageSender;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.flash.handler.netty.client.param.TcpClientParams;
import org.qiunet.flash.handler.netty.client.tcp.NettyTcpClient;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
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
	private static final NettyTcpClient tcpClient = NettyTcpClient.create(TcpClientParams.custom().setProtocolHeaderType(ProtocolHeaderType.node).build(), new TcpNodeClientTrigger());
	/**
	 * session
	 */
	private final DSession session;
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

		this.session = tcpClient.connect(serverInfo.getHost(), serverInfo.getServerPort(), f -> f.channel().attr(ServerConstants.MESSAGE_ACTOR_KEY).set(actor));
		session.sendMessage(CrossPlayerAuthRequest.valueOf(actor.getId(), ServerNodeManager.getCurrServerId()));
		this.serverId = serverId;
	}

	public int getServerId() {
		return serverId;
	}

	/**
	 * 获得session
	 * @return
	 */
	public DSession getSession() {
		return session;
	}

	@Override
	public IChannelMessageSender getSender() {
		return session;
	}
}
