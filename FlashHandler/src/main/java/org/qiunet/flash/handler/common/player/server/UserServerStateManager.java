package org.qiunet.flash.handler.common.player.server;

import com.google.common.collect.Maps;
import org.qiunet.cross.event.CrossEventManager;
import org.qiunet.cross.event.CrossEventRequest;
import org.qiunet.cross.node.ServerInfo;
import org.qiunet.cross.node.ServerNodeManager;
import org.qiunet.cross.rpc.IRpcFunction;
import org.qiunet.cross.rpc.IRpcRequest;
import org.qiunet.cross.rpc.RpcManager;
import org.qiunet.data.core.support.redis.IRedisUtil;
import org.qiunet.data.db.loader.event.PlayerKickOutEvent;
import org.qiunet.data.util.RedisMapUtil;
import org.qiunet.flash.handler.common.player.AbstractUserActor;
import org.qiunet.flash.handler.common.player.IPlayer;
import org.qiunet.flash.handler.common.player.UserOnlineManager;
import org.qiunet.flash.handler.common.player.event.*;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.flash.handler.netty.server.event.GlobalRedisPrepareEvent;
import org.qiunet.utils.listener.event.EventHandlerWeightType;
import org.qiunet.utils.listener.event.EventListener;
import org.qiunet.utils.listener.event.ICrossListenerEvent;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.string.StringUtil;
import org.slf4j.Logger;
import redis.clients.jedis.params.SetParams;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/***
 * 玩家的服务器状态管理.
 * 具体为: 分配的组  当前所在serverId  是否在线.
 *
 * @author qiunet
 * 2023/5/10 13:53
 */
public enum UserServerStateManager {
	instance;
	private static final Logger logger = LoggerType.DUODUO_FLASH_HANDLER.getLogger();
	private static final String USER_TEMP_SERVER_ID_REDIS_KEY = "user_temp_server_id_";
	private static final String SERVER_ID = "serverId";
	private static final String ONLINE = "online";
	private IRedisUtil redisUtil;

	@EventListener
	private void redisPrepare(GlobalRedisPrepareEvent event) {
		this.redisUtil = event.getGlobalRedis();
	}

	/**
	 * 根据 playerId 获得玩家的server state
	 * @param playerId 玩家id
	 * @return UserServerState
	 */
	public UserServerState getUserServerState(long playerId) {
		String redisKey = UserServerState.redisKey(playerId);
		Map<String, String> map = redisUtil.returnJedis().hgetAll(redisKey);
		if (map == null || map.isEmpty()) {
			return null;
		}
		UserServerState serverState = RedisMapUtil.toObj(map, UserServerState.class);
		serverState.redisKey = redisKey;
		serverState.playerId = playerId;
		return serverState;
	}

	/**
	 * 通过指定玩家线程 进行 rpc调用
	 * @param req 请求
	 * @param reqData 请求数据
	 * @param consumer 响应消费
	 * @param <E> 请求对象类型
	 * @param <R> 响应类型
	 */
	public <E extends IRpcRequest & IPlayer, R> void rpcCall(IRpcFunction<E, R> req, E reqData, BiConsumer<R, Throwable> consumer) {
		// rpc 调用
		try {
			RpcManager.rpcCall(this.assignServerId(reqData.getId()), req, reqData, consumer);
		} catch (NoRegisterException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 给playerId 分配一个server 进行事情.
	 * 有在某个服务器. 直接返回server id, 否则根据group id 重新分配.
	 * @param playerId 玩家id
	 * @return server id
	 */
	public int assignServerId(long playerId) throws NoRegisterException {
		return assignServerId(playerId, false);
	}

	/**
	 * 给playerId 分配一个server 进行事情.
  	 * 有在某个服务器. 直接返回server id,
	 * 否则 ! onlineOnly情况下, 根据group id 重新分配.
	 * @param playerId 玩家id
	 * @param onlineOnly 是否只返回在线的
	 * @return server id
	 */
	public int assignServerId(long playerId, boolean onlineOnly) throws NoRegisterException {
		UserServerState state = getUserServerState(playerId);
		if (state == null) {
			throw new NoRegisterException("Player "+playerId+" not register on logic server!");
		}

		if (onlineOnly && ! state.isOnline()) {
			logger.error("Player {} not online!", playerId);
			return 0;
		}

		int serverId = state.getServerId();
		if (serverId != 0) {
			return serverId;
		}

		ServerInfo serverInfo = ServerNodeManager.assignLogicServerByGroupId(state.getGroupId());
		if (serverInfo == null) {
			return 0;
		}

		String redisKey = USER_TEMP_SERVER_ID_REDIS_KEY + playerId;

		String succ = redisUtil.returnJedis().set(redisKey, String.valueOf(serverInfo.getServerId()), SetParams.setParams()
			.nx().ex(TimeUnit.MINUTES.toSeconds(1)));

		if (Objects.equals("OK", succ)) {
			return serverInfo.getServerId();
		}

		serverId = Integer.parseInt(redisUtil.returnJedis().get(redisKey));
		if (redisUtil.returnJedis().exists(ServerInfo.serverInfoRedisKey(serverId))) {
			return serverId;
		}

		// 可能服务器下线了.
		redisUtil.returnJedis().del(redisKey);
		return assignServerId(playerId, false);
	}

	/**
	 * 触发一个在线玩家的事件
	 * @param event 事件
	 * @param playerId 玩家id
	 * @param <E> 事件类型
	 */
	public <E extends PlayerEvent & ICrossListenerEvent> void fireOnlineEvent(E event, long playerId) {
		this.fireUserEvent0(event, playerId, true);
	}

	/**
	 * 触发玩家事件 如果不在线. 会以离线actor处理
	 * @param event 事件
	 * @param playerId 玩家id
	 * @param <E> 事件类型
	 */
	public  <E extends UserEvent & ICrossListenerEvent> void fireUserEvent(E event, long playerId) {
		this.fireUserEvent0(event, playerId, false);
	}

	private <E extends UserEvent & ICrossListenerEvent> void fireUserEvent0(E event, long playerId, boolean onlineOnly) {
		AbstractUserActor<?> actor = UserOnlineManager.instance.getActor(playerId);
		// 判断在本服
		if (actor != null) {
			if (actor.isPlayerActor()) {
				actor.addMessage(a -> {
					event.setPlayer(a).fireEventHandler();
				});
			} else {
				CrossEventRequest request = CrossEventRequest.valueOf(event);
				//  通过现有的玩家对象发跨服事件回去.
				actor.sendMessage(request, true);
			}
			return;
		}

		int serverId = 0;
		try {
			serverId = this.assignServerId(playerId, onlineOnly);
		} catch (NoRegisterException e) {
			logger.error("fire user event error: ", e);
		}

		if (serverId == 0) {
			return;
		}

		CrossEventManager.fireCrossUserEvent(serverId, event, playerId);
	}


	@EventListener(EventHandlerWeightType.LOWEST)
	private void userOnline(LoginSuccessEvent event) {
		if (!event.getPlayer().isPlayerActor()) {
			return;
		}

		if (! event.getPlayer().getSession().isActive()) {
			return;
		}

		UserServerState userServerState = getUserServerState(event.getPlayer().getId());
		String randomString = StringUtil.randomString(8);

		if (userServerState == null) {
			userServerState = UserServerState.onlineData(event.getPlayer().getId(), randomString);
			redisUtil.returnJedis().hmset(userServerState.getRedisKey(), RedisMapUtil.toMap(userServerState));
			return;
		}

		// 在serverId 踢出该用户
		if (userServerState.getServerId() > 0) {
			CrossEventManager.fireCrossEvent(userServerState.getServerId(), PlayerKickOutEvent.valueOf(event.getPlayer().getId()));
		}

		String redisKey = userServerState.getRedisKey();
		Map<String, String> map = Maps.newHashMap();
		map.put(SERVER_ID, String.valueOf(ServerNodeManager.getCurrServerId()));
		map.put(ONLINE, randomString);
		redisUtil.returnJedis().hmset(redisKey, map);

		// 登出就移除在线标志
		((DSession) event.getPlayer().getSession()).channel().closeFuture().addListener(f -> {
			redisUtil.execCommands(jedis -> {
				String string = jedis.hget(redisKey, ONLINE);
				if (Objects.equals(randomString, string)) {
					jedis.hdel(redisKey, ONLINE);
				}
				return null;
			});
		});
	}

	@EventListener
	private void userDestroy(PlayerDestroyEvent event) {
		if (! event.getPlayer().isPlayerActor()) {
			return;
		}

		String redisKey = UserServerState.redisKey(event.getPlayer().getId());
		redisUtil.returnJedis().hdel(redisKey, SERVER_ID);
	}

	@EventListener
	private void offlineUserCreate(OfflineUserCreateEvent event) {
		UserServerState serverState = getUserServerState(event.getPlayer().getId());
		assert serverState != null;
		if (serverState.getServerId() > 0) {
			logger.error("Handler offline player {} in wrong server!", event.getActor().getId());
			return;
		}
		redisUtil.returnJedis().hset(serverState.getRedisKey(), SERVER_ID, String.valueOf(ServerNodeManager.getCurrServerId()));
	}

	@EventListener
	private void offlineUserDestroy(OfflineUserDestroyEvent event) {
		UserServerState serverState = getUserServerState(event.getPlayer().getId());
		// OfflinePlayerActor 被踢出的时候. 会多一次. 这个没啥问题.
		if (serverState != null && serverState.getServerId() == ServerNodeManager.getCurrServerId()) {
			String redisKey = UserServerState.redisKey(event.getPlayer().getId());
			redisUtil.returnJedis().hdel(redisKey, SERVER_ID);
		}
	}
}
