package cc.alcina.framework.gwt.client.util;

import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.util.Timer;

public class TimerGwt implements Timer {
	public void cancel() {
		if (gwtTimer != null) {
			gwtTimer.cancel();
		}
	}

	public void schedule(int delayMillis) {
		gwtTimer().schedule(delayMillis);
	}

	private com.google.gwt.user.client.Timer gwtTimer() {
		gwtTimer = new com.google.gwt.user.client.Timer() {
			@Override
			public void run() {
				runnable.run();
			}
		};
		return gwtTimer;
	}

	public void scheduleRepeating(int periodMillis) {
		gwtTimer().scheduleRepeating(periodMillis);
	}

	private Runnable runnable;

	private com.google.gwt.user.client.Timer gwtTimer;

	private TimerGwt(Runnable runnable) {
		this.runnable = runnable;
	}

	@Override
	public void scheduleRepeating(long periodMillis) {
		scheduleRepeating((int) periodMillis);
	}

	@Override
	public void schedule(long delayMillis) {
		schedule((int) delayMillis);
	}

	public static class Provider implements Timer.Provider {
		@Override
		public Timer getTimer(Runnable runnable) {
			return new TimerGwt(runnable);
		}
	}

	@Override
	public void scheduleDeferred() {
		Scheduler.get().scheduleDeferred(runnable::run);
	}

	@Override
	public void scheduleDeferredIfOnUIThread() {
		Scheduler.get().scheduleDeferred(runnable::run);
	}
}
