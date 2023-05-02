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

	public static Runnable castIfRunnable(Object instance) {
		if (instance instanceof Runnable) {
			return (Runnable) instance;
		}
		if (instance instanceof ThrowingRunnable) {
			return asRunnable((ThrowingRunnable) instance);
		}
		return null;
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

	public static ThrowingRunnable wrapRunnable(Runnable runnable) {
		return new ThrowingRunnable() {
			@Override
			public void run() throws Exception {
				runnable.run();
			}
		};
	}

	public void run() throws Exception;

	default ThrowingSupplier<Void> asSupplier() {
		return () -> {
			run();
			return null;
		};
	}
}