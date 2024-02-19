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

	private java.util.Timer timer = new java.util.Timer(true);

	@Override
	public void scheduleDeferred() {
		schedule(1);
	}

	@Override
	public void scheduleDeferredIfOnUIThread() {
		// assume we're not
		runnable.run();
	}

	public static class Provider implements Timer.Provider {
		public Provider() {
		}

		@Override
		public Timer getTimer(Runnable runnable) {
			return new TimerJvm(runnable);
		}
	}

	private TimerTask task;

	private Runnable runnable;

	private TimerJvm(final Runnable runnable) {
		this.runnable = runnable;
		task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (Throwable e) {
					e.printStackTrace();
					topicWrapperException.publish(e);
				}
			}
		};
	}

	@Override
	public void cancel() {
		task.cancel();
		timer.cancel();
	}

	@Override
	public void scheduleRepeating(long periodMillis) {
		timer.scheduleAtFixedRate(task, periodMillis, periodMillis);
	}

	@Override
	public void schedule(long delayMillis) {
		timer.schedule(task, delayMillis);
	}
}