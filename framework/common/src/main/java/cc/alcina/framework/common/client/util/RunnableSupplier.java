package cc.alcina.framework.common.client.util;

import java.util.function.Supplier;

public class RunnableSupplier implements Supplier<Void> {
	public static RunnableSupplier of(Runnable runnable) {
		return new RunnableSupplier(runnable);
	}

	private Runnable runnable;

	RunnableSupplier(Runnable runnable) {
		this.runnable = runnable;
	}

	@Override
	public Void get() {
		runnable.run();
		return null;
	}
}