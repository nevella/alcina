package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.Timer;

import cc.alcina.framework.common.client.util.TimerWrapper;

public class TimerWrapperGwt extends Timer implements TimerWrapper {
	private Runnable runnable;

	private TimerWrapperGwt(Runnable runnable) {
		this.runnable = runnable;
	}

	@Override
	public void run() {
		runnable.run();
	}

	@Override
	public void scheduleRepeating(long periodMillis) {
		scheduleRepeating((int) periodMillis);
	}

	@Override
	public void scheduleSingle(long delayMillis) {
		schedule((int) delayMillis);
	}

	public static class TimerWrapperProviderGwt
			implements TimerWrapperProvider {
		@Override
		public TimerWrapper getTimer(Runnable runnable) {
			return new TimerWrapperGwt(runnable);
		}

		@Override
		public void scheduleDeferred(Runnable runnable) {
			getTimer(runnable).scheduleSingle(1);
		}

		@Override
		public void scheduleDeferredIfOnUIThread(Runnable runnable) {
			scheduleDeferred(runnable);
		}
	}
}
