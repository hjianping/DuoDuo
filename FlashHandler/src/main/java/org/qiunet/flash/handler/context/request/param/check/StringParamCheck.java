package org.qiunet.flash.handler.context.request.param.check;

import org.qiunet.cfg.manager.keyval.KeyValManager;
import org.qiunet.flash.handler.context.request.data.IChannelData;
import org.qiunet.flash.handler.context.status.IGameStatus;
import org.qiunet.flash.handler.context.status.StatusResultException;
import org.qiunet.function.badword.BadWordFilter;
import org.qiunet.utils.reflect.ReflectUtil;
import org.qiunet.utils.string.StringUtil;

import java.lang.reflect.Field;

/***
 * 字符串参数检查
 *
 * @author qiunet
 * 2022/1/5 18:01
 */
class StringParamCheck implements IParamCheck {
	private final Field field;
	/**
	 * 自定义 最小值
	 */
	private final long min;
	/**
	 * 自定义 最大值
	 */
	private final long max;
	/**
	 * 检查空
	 */
	private final boolean trim;
	/**
	 * 检查空
	 */
	private final boolean checkEmpty;
	/**
	 * 检查关键字
	 */
	private final boolean checkBadWorld;

	public StringParamCheck(Field field) {
		this.field = field;

		StringParam param = this.field.getAnnotation(StringParam.class);
		this.max = KeyValManager.instance.getLong(param.maxKey(), param.max());
		this.min = KeyValManager.instance.getLong(param.minKey(), param.min());

		this.checkBadWorld = param.checkBadWord();
		this.checkEmpty = param.checkEmpty();
		this.trim = param.trim();
	}

	@Override
	public void check(IChannelData data) {
		String val = (String) ReflectUtil.getField(this.field, data);
		if (this.trim && !StringUtil.isEmpty(val)) {
			val = StringUtil.powerfulTrim(val);
			ReflectUtil.setField(data, field, val);
		}

		if (checkEmpty && StringUtil.isEmpty(val)) {
			throw StatusResultException.valueOf(IGameStatus.STRING_PARAM_EMPTY_ERROR);
		}

		if (min != 0 &&  val.length() < min) {
			throw StatusResultException.valueOf(IGameStatus.STRING_PARAM_LENGTH_ERROR);
		}

		if (max != 0 && val.length() > max) {
			throw StatusResultException.valueOf(IGameStatus.STRING_PARAM_LENGTH_ERROR);
		}

		if (checkBadWorld && BadWordFilter.instance.powerFind(val) != null) {
			throw StatusResultException.valueOf(IGameStatus.STRING_PARAM_BAD_WORD_ERROR);
		}
	}
}
