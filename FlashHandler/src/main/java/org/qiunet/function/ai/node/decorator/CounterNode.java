package org.qiunet.function.ai.node.decorator;

import com.google.common.base.Preconditions;
import org.qiunet.function.ai.enums.ActionStatus;
import org.qiunet.function.ai.node.IBehaviorNode;
import org.qiunet.function.ai.node.base.BaseDecorator;

import java.util.concurrent.atomic.AtomicInteger;

/***
 * 计数节点. 如果完成次数达到指定数量.
 * 比如登录. 生命周期只需要一次.
 * 之后直接返回false
 * qiunet
 * 2021/8/16 21:40
 **/
public class CounterNode<Owner> extends BaseDecorator<Owner> {
	private final AtomicInteger currCount = new AtomicInteger();
	private final int count;

	public CounterNode(IBehaviorNode<Owner> node, int count) {
		super(node);
		Preconditions.checkArgument(count > 0, "count [%s] less than 1!");
		this.count = count;
	}

	@Override
	protected ActionStatus execute() {
		ActionStatus status = node.run();
		if (status == ActionStatus.SUCCESS) {
			currCount.incrementAndGet();
		}
		return status;
	}

	@Override
	public boolean preCondition() {
		if (currCount.get() >= count) {
			return false;
		}
		return super.preCondition();
	}
}
