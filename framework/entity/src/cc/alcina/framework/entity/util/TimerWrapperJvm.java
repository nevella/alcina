package cc.alcina.framework.entity.util;

import java.util.Timer;
import java.util.TimerTask;

import cc.alcina.framework.common.client.util.TimerWrapper;

/**
 * XP (JVM/GWT) timer functionality
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class TimerWrapperJvm extends Timer implements TimerWrapper {
	private Runnable runnable;

	private TimerWrapperJvm(Runnable runnable) {
		this.runnable = runnable;
	}

	public static class TimerWrapperProviderJvm implements TimerWrapperProvider {
		@Override
		public TimerWrapper getTimer(Runnable runnable) {
			return new TimerWrapperJvm(runnable);
		}

		@Override
		public void scheduleDeferred(Runnable runnable) {
			getTimer(runnable).scheduleSingle(1);
		}
	}

	@Override
	public void scheduleRepeating(long periodMillis) {
		scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				runnable.run();
			}
		}, periodMillis, periodMillis);
	}

	@Override
	public void scheduleSingle(long delayMillis) {
		schedule(new TimerTask() {
			@Override
			public void run() {
				runnable.run();
			}
		}, delayMillis);
	}
}
