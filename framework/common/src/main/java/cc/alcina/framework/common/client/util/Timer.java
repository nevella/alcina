package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * This behaves more like a gwt than a jdk timer - i.e. it delegates the
 * scheduling to something else (to maintain a single timer thread on the jdk)
 */
public interface Timer {
	void cancel();

	void scheduleRepeating(long periodMillis);

	void schedule(long delayMillis);

	void scheduleDeferred();

	void scheduleDeferredIfOnUIThread();

	public static Timer scheduleDelayed(Runnable runnable, long delayMillis) {
		Timer timer = Provider.get().getTimer(runnable);
		timer.schedule(delayMillis);
		return timer;
	}

	public static Timer scheduleRepeating(Runnable runnable,
			long periodMillis) {
		Timer timer = Provider.get().getTimer(runnable);
		timer.scheduleRepeating(periodMillis);
		return timer;
	}

	@Reflected
	public interface Provider {
		public static Timer.Provider get() {
			return Registry.impl(Timer.Provider.class);
		}

		Timer getTimer(Runnable runnable);
	}
}
