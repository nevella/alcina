package cc.alcina.framework.entity.util;

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