package org.qiunet.utils.nonSyncQuene;


public class TestElement implements QueueElement {
	private String threadName;
	public TestElement(String name){
		this.threadName = name;
	}
	@Override
	public boolean handler() {
		return true;
	}
	@Override
	public String toStr() {
		return "["+threadName+"]";
	}
}
