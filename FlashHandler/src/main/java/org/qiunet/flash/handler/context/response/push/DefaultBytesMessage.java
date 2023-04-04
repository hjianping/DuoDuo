package org.qiunet.flash.handler.context.response.push;

import java.nio.ByteBuffer;

/***
 *
 * @Author qiunet
 * @Date Create in 2018/5/26 23:49
 **/
public class DefaultBytesMessage extends ExtraInfo implements IChannelMessage<byte []> {

	private final int protocolId;

	private final ByteBuffer buffer;

	public DefaultBytesMessage(int protocolId, byte[] message) {
		this.buffer = ByteBuffer.wrap(message);
		this.protocolId = protocolId;
	}

	@Override
	public boolean debugOut() {
		return false;
	}

	@Override
	public int getProtocolID() {
		return protocolId;
	}

	@Override
	public byte[] getContent() {
		return buffer.array();
	}

	@Override
	public String _toString() {
		return "ProtocolID: "+protocolId;
	}

	@Override
	public ByteBuffer byteBuffer() {
		return buffer;
	}
}
