package org.qiunet.log.record.content;

import com.google.common.collect.Maps;
import org.qiunet.log.record.msg.ILogRecordMsg;
import org.qiunet.utils.exceptions.CustomException;

import java.util.Map;

/***
 * 获取 map 类型 日志内容
 * 业务确定了后, 可以用常量管理实例
 *
 * @author qiunet
 * 2022/11/25 10:55
 */
public class MapLogContentGetter implements ILogContentGetter<Map<String, Object>> {

	@Override
	public Map<String, Object> getData(ILogRecordMsg<?> msg) {
		Map<String, Object> map = Maps.newHashMap();
		msg.forEachData(row -> {
			Object o = map.putIfAbsent(row.getKey(), row.getVal());
			if (o != null) {
				throw new CustomException("key {} repeated!", row.getKey());
			}
		});
		return map;
	}
}
