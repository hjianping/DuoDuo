package org.qiunet.flash.handler.netty.client.param;

import org.qiunet.flash.handler.context.header.IProtocolHeaderType;
import org.qiunet.flash.handler.context.header.ProtocolHeaderType;

import java.net.InetSocketAddress;

/**
 * Created by qiunet.
 * 17/11/22
 */
public abstract class AbstractClientParam implements IClientConfig {
	/***
	 * 地址
	 */
	protected InetSocketAddress address;

	protected boolean encryption = true;

	protected int maxReceivedLength = 1024 * 1024 * 8;

	protected IProtocolHeaderType protocolHeaderType = ProtocolHeaderType.client;

	@Override
	public IProtocolHeaderType getProtocolHeaderType() {
		return protocolHeaderType;
	}

	@Override
	public InetSocketAddress getAddress() {
		return address;
	}

	@Override
	public boolean isEncryption() {
		return encryption;
	}

	@Override
	public int getMaxReceivedLength() {
		return maxReceivedLength;
	}

	/***
	 * 使用build模式 set和 get 分离. 以后有有顺序的构造时候也可以不动
	 * */
	public abstract class SuperBuilder<P extends AbstractClientParam, B extends SuperBuilder> {
		public B setAddress(InetSocketAddress address) {
			AbstractClientParam.this.address = address;
			return (B) this;
		}
		public B setAddress(String host, int port) {
			return setAddress(new InetSocketAddress(host, port));
		}
		/**
		 * 设置
		 * @param protocolHeaderAdapter
		 * @return
		 */
		public B setProtocolHeaderType(IProtocolHeaderType protocolHeaderAdapter) {
			AbstractClientParam.this.protocolHeaderType = protocolHeaderAdapter;
			return (B) this;
		}

		public B setMaxReceivedLength(int maxReceivedLength) {
			AbstractClientParam.this.maxReceivedLength = maxReceivedLength;
			return (B) this;
		}

		public B setEncryption(boolean encryption) {
			AbstractClientParam.this.encryption = encryption;
			return (B) this;
		}

		/**
		 * 构造build
		 * @return
		 */
		public P build(){
			return newParams();
		}

		/**
		 * 得到一个新的param
		 * @return
		 */
		protected abstract P newParams();
	}
}
