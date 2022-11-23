package org.qiunet.log.record.logger;

import org.qiunet.log.record.enums.ILogRecordType;
import org.qiunet.log.record.msg.ILogRecordMsg;

/***
 *
 * 自己的一个通用logger接口.
 * 子类可以是TcpRecordLogger UdpRecordLogger LogBackRecordLogger
 * @author qiunet
 * 2020-03-25 10:36
 ***/
interface IBasicRecordLogger<D> {
	/**
	 * logger 的名称
	 * @return
	 */
	 String recordLoggerName();

	/**
	 * 记录日志
	 * @param logRecordMsg
	 */
	 <T extends Enum<T> & ILogRecordType<T>, L extends ILogRecordMsg<T, D>> void send(L logRecordMsg);
}
