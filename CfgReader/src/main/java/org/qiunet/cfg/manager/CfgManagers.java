package org.qiunet.cfg.manager;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import org.qiunet.cfg.event.CfgLoadCompleteEvent;
import org.qiunet.cfg.event.CfgManagerAddEvent;
import org.qiunet.cfg.event.CfgPrepareOverEvent;
import org.qiunet.cfg.manager.base.ICfgManager;
import org.qiunet.utils.exceptions.CustomException;
import org.qiunet.utils.listener.event.EventListener;
import org.qiunet.utils.logger.LoggerType;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 总管 游戏设定加载
 * @author qiunet
 *         Created on 17/2/9 12:15.
 */
public enum CfgManagers {
	INSTANCE,;
	private final Comparator<ICfgManager<?, ?>> comparator = ((o1, o2) -> ComparisonChain.start().compare(o2.order(), o1.order()).result());
	private final List<ICfgManager<?, ?>> gameSettingList = Lists.newArrayListWithCapacity(100);
	private final Logger logger = LoggerType.DUODUO_CFG_READER.getLogger();
	private final AtomicBoolean reloading = new AtomicBoolean();

	public static CfgManagers getInstance(){
		return INSTANCE;
	}

	/**
	 * 初始化会比重新加载多一层排序
	 */
	@EventListener
	private void initSetting(CfgPrepareOverEvent event) {
		gameSettingList.sort(comparator);
		this.reloadSetting(gameSettingList, false);
	}

	/***
	 * 重新加载 指定cfg Manager
	 * 文件变动热更使用
	 */
	public synchronized void reloadSetting(List<ICfgManager<?, ?>> gameSettingList) {
		gameSettingList.sort(comparator);
		this.reloadSetting(gameSettingList, true);
	}
	/***
	 * 重新加载
	 */
	public synchronized void reloadSetting() {
		this.reloadSetting(gameSettingList, true);
	}

	private synchronized void reloadSetting(List<ICfgManager<?, ?>> gameSettingList, boolean needLogger) {
		if (reloading.get()) {
			logger.error("Game Setting Data is loading now.....");
			return;
		}

		logger.error("Game Setting Data Load start.....");
		try {
			reloading.compareAndSet(false, true);
			this.loadDataSetting(gameSettingList);
			// 通知更新完毕
			CfgLoadCompleteEvent.valueOf(gameSettingList).fireEventHandler();
		}catch (CustomException e) {
			if (needLogger) {
				e.logger(logger);
			}
			throw e;
		} finally {
			reloading.compareAndSet(true, false);
		}
		logger.error("Game Setting Data Load over.....");
	}

	/**
	 * 添加 Manager
	 */
	@EventListener
	private void addCfgManager(CfgManagerAddEvent event) {
		this.gameSettingList.add(event.getCfgManager());
	}

	/***
	 * 加载设定文件
	 * @return 返回加载失败的文件名称
	 */
	private synchronized void loadDataSetting(List<ICfgManager<?, ?>> gameSettingList) {
		for (ICfgManager<?, ?> cfgManager : gameSettingList) {
			try {
				cfgManager.loadCfg();
			}catch (Exception e) {
				throw new CustomException(e, "读取配置文件 [{}]({}) 失败!", cfgManager.getCfgClass().getSimpleName(), cfgManager.getLoadFileName());
			}
			logger.info("Load Config [{}]({})", cfgManager.getCfgClass().getSimpleName(), cfgManager.getLoadFileName());
		}
	}
}
