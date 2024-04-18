package cc.alcina.framework.entity.util;

import java.util.TimerTask;

import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.common.client.util.Topic;

/**
 * XP (JVM/GWT) timer functionality
 *
 * 
 *
 */
/*
 * will always be a singleton
 */
public class TimerJvm implements Timer {
	public static final Topic<Throwable> topicWrapperException = Topic.create();

	@Override
	public void scheduleDeferred() {
		schedule(1);
	}

	@Override
	public void scheduleDeferredIfOnUIThread() {
		// assume we're not
		runnable.run();
	}

	/*
	 * Provides a single timer
	 */
	public static class Provider implements Timer.Provider {
		public Provider() {
		}

		private java.util.Timer timer = new java.util.Timer(
				"alcina-timerjvm-provider", true);

		@Override
		public Timer getTimer(Runnable runnable) {
			return new TimerJvm(timer, runnable);
		}
	}

	private TimerTask task;

	private Runnable runnable;

	private java.util.Timer timer;

	private TimerJvm(java.util.Timer timer, final Runnable runnable) {
		this.timer = timer;
		this.runnable = runnable;
		task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
					maybeFinish();
				} catch (Throwable e) {
					e.printStackTrace();
					topicWrapperException.publish(e);
				}
			}
		};
	}

	private void maybeFinish() {
		if (periodMillis == 0) {
			task.cancel();
		}
	}

	@Override
	public void cancel() {
		task.cancel();
	}

	long periodMillis;

	@Override
	public void scheduleRepeating(long periodMillis) {
		this.periodMillis = periodMillis;
		timer.scheduleAtFixedRate(task, periodMillis, periodMillis);
	}

	@Override
	public void schedule(long delayMillis) {
		timer.schedule(task, delayMillis);
	}
}