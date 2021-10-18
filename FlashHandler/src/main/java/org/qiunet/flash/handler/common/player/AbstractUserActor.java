package org.qiunet.flash.handler.common.player;

import org.qiunet.flash.handler.common.player.event.BaseUserEventData;
import org.qiunet.flash.handler.common.player.event.PlayerLogoutEventData;
import org.qiunet.flash.handler.context.session.DSession;
import org.qiunet.utils.listener.event.EventManager;

/***
 * 玩家类型的messageActor 继承该类
 *
 * @author qiunet
 * 2020-10-13 20:51
 */
public abstract class AbstractUserActor<T extends AbstractUserActor<T>> extends AbstractMessageActor<T>  {

	public AbstractUserActor(DSession session) {
		super(session);
	}
	/**
	 * 是否跨服状态
	 *
	 * @return
	 */
	public abstract boolean isCrossStatus();

	@Override
	protected void setSession(DSession session) {
		this.session = session;
		this.session.addCloseListener(cause -> this.fireEvent(new PlayerLogoutEventData<T>((T) this, cause)));
	}

	/**
	 * 对事件的处理.
	 * 跨服的提交跨服那边. 本服调用自己的
	 * @param eventData
	 */
	public <D extends BaseUserEventData<T>> void fireEvent(D eventData){
		eventData.setPlayer((T) this);
		EventManager.fireEventHandler(eventData);
	}
}
