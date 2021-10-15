package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface TimerWrapper {
	public void cancel();

	public void scheduleRepeating(long periodMillis);

	public void scheduleSingle(long delayMillis);

	public interface TimerWrapperProvider {
		public static TimerWrapperProvider get() {
			return Registry.impl(TimerWrapperProvider.class);
		}

		public TimerWrapper getTimer(Runnable runnable);

		public void scheduleDeferred(Runnable runnable);

		public void scheduleDeferredIfOnUIThread(Runnable runnable);
	}
}
