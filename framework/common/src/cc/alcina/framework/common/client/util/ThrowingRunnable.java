package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;

@FunctionalInterface
public interface ThrowingRunnable {
	public static List<Callable>
			asCallables(Collection<ThrowingRunnable> throwingRunnables) {
		return throwingRunnables.stream().map(CallableImpl::new)
				.collect(Collectors.toList());
	}

	public static List<Runnable>
			asRunnables(List<ThrowingRunnable> throwingRunnables) {
		return throwingRunnables.stream().map(WrappingRunnableImpl::new)
				.collect(Collectors.toList());
	}

	public static void runAll(Collection<ThrowingRunnable> throwingRunnables) {
		try {
			for (ThrowingRunnable throwingRunnable : throwingRunnables) {
				throwingRunnable.run();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void run() throws Exception;

	static class CallableImpl implements Callable {
		private ThrowingRunnable throwingRunnable;

		CallableImpl(ThrowingRunnable throwingRunnable) {
			this.throwingRunnable = throwingRunnable;
		}

		@Override
		public Object call() throws Exception {
			throwingRunnable.run();
			return null;
		}
	}

	static class WrappingRunnableImpl implements Runnable {
		private ThrowingRunnable throwingRunnable;

		WrappingRunnableImpl(ThrowingRunnable throwingRunnable) {
			this.throwingRunnable = throwingRunnable;
		}

		@Override
		public void run() {
			try {
				throwingRunnable.run();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}