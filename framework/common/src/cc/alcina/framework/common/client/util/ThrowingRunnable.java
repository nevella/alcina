package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;

@FunctionalInterface
public interface ThrowingRunnable {
	public static Runnable asRunnable(ThrowingRunnable runnable) {
		return () -> ThrowingRunnable.wrap(runnable);
	}

	public static List<Runnable>
			asRunnables(List<ThrowingRunnable> throwingRunnables) {
		return throwingRunnables.stream().map(ThrowingRunnable::asRunnable)
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

	public static void wrap(ThrowingRunnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public void run() throws Exception;
}