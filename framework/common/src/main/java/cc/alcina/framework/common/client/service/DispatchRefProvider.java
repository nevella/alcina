package cc.alcina.framework.common.client.service;

import java.util.function.Consumer;

import cc.alcina.framework.common.client.context.LooseContext;

public class DispatchRefProvider {
	public static LooseContext.Key<DispatchRefProvider> context = LooseContext
			.key(DispatchRefProvider.class, "context");

	public static DispatchRefProvider get() {
		return context.optional().orElse(new DispatchRefProvider());
	}

	public Consumer<Runnable> getDispatch() {
		return Runnable::run;
	}
}