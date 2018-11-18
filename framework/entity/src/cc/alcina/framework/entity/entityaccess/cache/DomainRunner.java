package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

public abstract class DomainRunner extends DomainReader<Void, Void> {
	public static void run(Runnable runnable) {
		new DomainRunner() {
			@Override
			protected void run() throws Exception {
				runnable.run();
			}
		};
	}

	public static void runThrowing(ThrowingRunnable runnable) {
		new DomainRunner() {
			@Override
			protected void run() throws Exception {
				runnable.run();
			}
		};
	}

	public DomainRunner() {
		try {
			read(null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	protected Void read0(Void input) throws Exception {
		run();
		return null;
	}

	protected abstract void run() throws Exception;
}
