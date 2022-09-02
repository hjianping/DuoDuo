package org.qiunet.flash.handler.netty.server.kcp.shakehands.message;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import org.qiunet.flash.handler.common.id.IProtocolId;
import org.qiunet.flash.handler.context.request.data.ChannelData;
import org.qiunet.flash.handler.context.request.data.IChannelData;

/***
 *
 * @author qiunet
 * 2022/4/27 11:06
 */
@ChannelData(ID = IProtocolId.System.KCP_TOKEN_RSP, desc = "kcp的token申请响应")
public class KcpTokenRsp extends IChannelData {

	@Protobuf(description = "用来做kcp关联,有效时间60秒!")
	private String token;
	@Protobuf(description = "会话ID")
	private int convId;
	@Protobuf(description = "端口")
	private int port;

	public static KcpTokenRsp valueOf(int convId, String token, int port) {
		KcpTokenRsp data = new KcpTokenRsp();
		data.convId = convId;
		data.token = token;
		data.port = port;
		return data;
	}

	public int getConvId() {
		return convId;
	}

	public void setConvId(int convId) {
		this.convId = convId;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
