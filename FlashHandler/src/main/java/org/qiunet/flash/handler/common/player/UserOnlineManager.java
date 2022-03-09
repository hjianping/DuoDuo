package org.qiunet.flash.handler.common.player;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.qiunet.cross.actor.CrossPlayerActor;
import org.qiunet.data.util.ServerConfig;
import org.qiunet.flash.handler.common.player.event.AuthEventData;
import org.qiunet.flash.handler.common.player.event.CrossPlayerDestroyEvent;
import org.qiunet.flash.handler.common.player.event.CrossPlayerLogoutEvent;
import org.qiunet.flash.handler.common.player.event.PlayerLogoutEventData;
import org.qiunet.flash.handler.common.player.observer.IPlayerDestroy;
import org.qiunet.flash.handler.netty.server.constants.CloseCause;
import org.qiunet.flash.handler.netty.server.constants.ServerConstants;
import org.qiunet.utils.async.future.DFuture;
import org.qiunet.utils.collection.enums.ForEachResult;
import org.qiunet.utils.listener.event.EventHandlerWeightType;
import org.qiunet.utils.listener.event.EventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/***
 * 用户的actor管理.
 * 包括功能服 和 跨服的.
 * 在功能服. 管理的是用户的真实连接.
 * 在跨服. 管理的是功能服代理的连接
 *
 * @author qiunet
 * 2020-10-16 10:25
 */
public enum UserOnlineManager {
	instance;
	/*监听*/
	private List<IOnlineUserSizeChangeListener> changeListeners;
	/**
	 * 在线
	 */
	private static final Map<Long, AbstractUserActor> onlinePlayers = Maps.newConcurrentMap();
	/**
	 * 等待重连
	 */
	private static final Map<Long, WaitActor> waitReconnects = Maps.newConcurrentMap();


	@EventListener
	private void addPlayerActor(AuthEventData eventData) {
		AbstractUserActor userActor = eventData.getPlayer();
		Preconditions.checkState(userActor.isAuth());
		onlinePlayers.put(userActor.getId(), userActor);
	}
	/**
	 * 玩家自主退出，会完整走登出流程
	 * @param actor 玩家
	 */
	public <T extends AbstractUserActor<T>> void playerQuit(T actor) {
		if (actor.isCrossPlayer()) {
			((CrossPlayerActor) actor).fireCrossEvent(CrossPlayerLogoutEvent.valueOf(ServerConfig.getServerId()));
		}

		actor.session.close(CloseCause.LOGOUT);
		this.destroyPlayer(actor);
	}
	/**
	 * 登出事件
	 * @param eventData
	 */
	@EventListener(EventHandlerWeightType.LESS)
	private <T extends AbstractUserActor<T>> void onLogout(PlayerLogoutEventData<T> eventData) {
		AbstractUserActor<T> userActor = eventData.getPlayer();
		T actor = (T) onlinePlayers.remove(userActor.getId());
		if (actor == null) {
			return;
		}
		triggerChangeListeners(false);
		// 清理 observers 避免重连重复监听.
		actor.getObserverSupport().clear(clz -> clz != IPlayerDestroy.class);
		// CrossPlayerActor 如果断连. 由playerActor维护心跳.
		if (actor.isCrossPlayer()) {
			return;
		}

		((PlayerActor) actor).dataLoader().syncToDb();
		if (eventData.getCause().needWaitConnect() && userActor.isAuth()) {
			// 给3分钟重连时间
			DFuture<Void> future = actor.scheduleMessage(p -> this.destroyPlayer(actor), 3, TimeUnit.MINUTES);
			waitReconnects.put(actor.getId(), new WaitActor(((PlayerActor) actor), future));
		}
	}
	/**
	 * 重连
	 * 重连需要把新的session换到旧的里面。 把channel里面换成旧的
	 * @param playerId 玩家id
	 * @param currActor 当前的actor
	 * @return null 说明不能重连了. 否则之后使用返回的actor进行操作.
	 */
	public PlayerActor reconnect(long playerId, PlayerActor currActor) {
		WaitActor waitActor = waitReconnects.remove(playerId);
		if (waitActor == null) {
			return null;
		}
		waitActor.actor.clearObservers();
		waitActor.actor.merge(currActor);
		waitActor.future.cancel(true);
		currActor.getSender().channel().attr(ServerConstants.MESSAGE_ACTOR_KEY).set(waitActor.actor);

		onlinePlayers.put(playerId, waitActor.actor);
		triggerChangeListeners(true);
		currActor.destroy();

		return waitActor.actor;
	}
	/**
	 * 玩家销毁， 销毁后，不可重连
	 * @param userActor
	 */
	private <T extends AbstractUserActor<T>> void destroyPlayer(T userActor) {
		userActor.getObserverSupport().syncFire(IPlayerDestroy.class, p -> p.destroyActor(userActor));
		if (userActor.isCrossPlayer() && userActor.getSender().isActive()) {
			((CrossPlayerActor) userActor).fireCrossEvent(CrossPlayerDestroyEvent.valueOf(ServerConfig.getServerId()));
		}
		waitReconnects.remove(userActor.getId());
		AbstractUserActor remove = onlinePlayers.remove(userActor.getId());
		if (remove != null) {
			triggerChangeListeners(false);
		}
		userActor.destroy();
	}
	/**
	 * 在线本服玩家数量
	 * @return 数量
	 */
	public int onlinePlayerSize(){
		return (int) onlinePlayers.values().stream().filter(AbstractUserActor::isPlayerActor).count();
	}
	/**
	 * 在线跨服玩家数量
	 * @return 数量
	 */
	public int crossPlayerSize(){
		return (int) onlinePlayers.values().stream().filter(AbstractUserActor::isCrossPlayer).count();
	}

	/**
	 * 所有在线的数量
	 * @return
	 */
	public int onlineSize(){
		return onlinePlayers.size();
	}
	/**
	 * 遍历在线玩家.
	 * @param consume
	 */
	public void foreach(Function<AbstractUserActor, ForEachResult> consume) {
		this.foreach(consume, null);
	}
	/**
	 * 遍历在线玩家.
	 * @param consume
	 */
	public void foreach(Function<AbstractUserActor, ForEachResult> consume, Predicate<AbstractUserActor> filter) {
		for (AbstractUserActor actor : onlinePlayers.values()) {
			if (filter != null && filter.test(actor)) {
				continue;
			}
			ForEachResult result = consume.apply(actor);
			if (result == ForEachResult.BREAK) {
				break;
			}
		}
	}
	/**
	 * 获得 Actor
	 * @param playerId
	 * @param <T>
	 * @return
	 */
	public static <T extends AbstractUserActor<T>> T getPlayerActor(long playerId) {
		return (T) onlinePlayers.get(playerId);
	}
	/**
	 * 得到等待重连的player
	 * @param playerId 玩家id
	 * @return playerActor
	 */
	public static PlayerActor getWaitReconnectPlayer(long playerId) {
		WaitActor waitActor = waitReconnects.get(playerId);
		return waitActor == null ? null : waitActor.actor;
	}

	private static class WaitActor {
		PlayerActor actor;
		DFuture<Void> future;

		public WaitActor(PlayerActor actor, DFuture<Void> future) {
			this.actor = actor;
			this.future = future;
		}
	}

	/**
	 * 添加变动监听
	 * @param listener
	 */
	public synchronized void addChangeListener(IOnlineUserSizeChangeListener listener) {
		if (this.changeListeners == null) {
			this.changeListeners = Lists.newCopyOnWriteArrayList();
		}
		this.changeListeners.add(listener);
	}

	/**
	 * 触发监听
	 * @return
	 */
	private void triggerChangeListeners(boolean add){
		if (changeListeners == null) {
			return;
		}
		changeListeners.forEach(listener -> listener.change(add));
	}
	/**
	 * 现在玩家变动监听
	 */
	@FunctionalInterface
	public interface IOnlineUserSizeChangeListener {
		/**
		 * @param add true 加 false 减
		 */
		void change(boolean add);
	}
}
