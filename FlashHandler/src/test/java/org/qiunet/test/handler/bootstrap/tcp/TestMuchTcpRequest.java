package org.qiunet.test.handler.bootstrap.tcp;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;
import org.qiunet.flash.handler.common.id.IProtocolId;
import org.qiunet.flash.handler.common.message.MessageContent;
import org.qiunet.flash.handler.context.header.SequenceIdProtocolHeader;
import org.qiunet.flash.handler.context.session.ClientSession;
import org.qiunet.flash.handler.context.session.ISession;
import org.qiunet.flash.handler.netty.client.param.TcpClientConfig;
import org.qiunet.flash.handler.netty.client.tcp.NettyTcpClient;
import org.qiunet.flash.handler.netty.client.trigger.IPersistConnResponseTrigger;
import org.qiunet.flash.handler.netty.server.message.ConnectionReq;
import org.qiunet.test.handler.proto.LoginResponse;
import org.qiunet.test.handler.proto.TcpPbLoginRequest;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.string.StringUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 大量请求的泄漏测试
 * Created by qiunet.
 * 17/11/27
 */
public class TestMuchTcpRequest extends BasicTcpBootStrap {
	private final int requestCount = 1000;
	private final int threadCount = 50;
	private final AtomicInteger counter = new AtomicInteger();
	private final CountDownLatch latch = new CountDownLatch(threadCount * requestCount);

	@Test
	public void muchRequest() throws InterruptedException {
		NettyTcpClient nettyTcpClient = NettyTcpClient.create(TcpClientConfig.custom().setProtocolHeader(SequenceIdProtocolHeader.instance).build(), new Trigger());
		long start = System.currentTimeMillis();

		for (int j = 0; j < threadCount; j++) {
			new Thread(() -> {
				ClientSession connector = nettyTcpClient.connect(host, port);
				connector.sendMessage(ConnectionReq.valueOf(StringUtil.randomString(10)));

				for (int i = 0 ; i < requestCount; i ++) {
					String text = "test [testTcpProtobuf]: "+i;
					TcpPbLoginRequest request = TcpPbLoginRequest.valueOf(text, text, 11, null);
					connector.sendMessage(request, true);
				}
			}).start();
		}

		latch.await();
		long end = System.currentTimeMillis();
		System.out.println("All Time is:["+(end - start)+"]ms");
	}

	public class Trigger implements IPersistConnResponseTrigger {
		@Override
		public void response(ISession session, Channel channel, MessageContent data) {
			if (data.getProtocolId() == IProtocolId.System.CONNECTION_RSP) {
				return;
			}
			LoginResponse response = data.decodeProtobuf(LoginResponse.class);
			LoggerType.DUODUO_FLASH_HANDLER.info("count: {}, content: {}", counter.incrementAndGet(), response.getTestString());
			latch.countDown();
		}
	}
}
