package cc.alcina.framework.common.client.util;

import java.util.concurrent.Callable;

public class RunnableCallable implements Callable<Void> {
	private Runnable runnable;

	public RunnableCallable(Runnable runnable) {
		this.runnable = runnable;
	}

	@Override
	public Void call() throws Exception {
		runnable.run();
		return null;
	}
}