package cc.alcina.framework.servlet.servlet.impl;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;

@Registration(
	value = InstanceOracle.ProviderInvoker.class,
	priority = Priority.PREFERRED_LIBRARY)
public class InstanceOracle_ProviderInvoker_Impl
		extends InstanceOracle.ProviderInvoker {
	@Override
	public void invoke(String name, Runnable runnable) {
		try {
			LooseContext.push();
			/*
			 * Key - to prevent context leakage (although we do want to preserve
			 * permissions context)
			 */
			LooseContext.getContext().clearProperties();
			AlcinaChildRunnable.runInTransactionNewThread(name,
					ThrowingRunnable.wrapRunnable(runnable));
		} finally {
			LooseContext.pop();
		}
	}
}
