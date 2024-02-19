package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface Timer {
	void cancel();

	void scheduleRepeating(long periodMillis);

	void schedule(long delayMillis);

	void scheduleDeferred();

	void scheduleDeferredIfOnUIThread();

	public interface Provider {
		public static Timer.Provider get() {
			return Registry.impl(Timer.Provider.class);
		}

		Timer getTimer(Runnable runnable);
	}
}
