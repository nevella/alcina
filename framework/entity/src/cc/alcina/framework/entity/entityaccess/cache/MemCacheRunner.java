package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

public abstract class MemCacheRunner extends MemCacheReader<Void, Void> {
	@Override
	protected Void read0(Void input) throws Exception {
		run();
		return null;
	}

	public MemCacheRunner() {
		try {
			read(null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void run() throws Exception;

	public static void run(Runnable runnable) {
		new MemCacheRunner() {
			@Override
			protected void run() throws Exception {
				runnable.run();
			}
		};
	}

	public static void runThrowing(ThrowingRunnable runnable) {
		new MemCacheRunner() {
			@Override
			protected void run() throws Exception {
				runnable.run();
			}
		};
	}
}
