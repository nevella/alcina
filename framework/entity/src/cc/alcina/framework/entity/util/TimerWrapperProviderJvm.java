package cc.alcina.framework.entity.util;

import java.util.Timer;
import java.util.TimerTask;

import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

/**
 * XP (JVM/GWT) timer functionality
 * 
 * @author nick@alcina.cc
 * 
 */
/*
 * will always be a singleton
 */
public class TimerWrapperProviderJvm implements TimerWrapperProvider {
	private Timer timer = new Timer(true);

	@Override
	public TimerWrapper getTimer(Runnable runnable) {
		return new TimerWrapperJvm(runnable);
	}

	@Override
	public void scheduleDeferred(Runnable runnable) {
		getTimer(runnable).scheduleSingle(1);
	}

	@Override
	public void scheduleDeferredIfOnUIThread(Runnable runnable) {
		// assume we're not
		runnable.run();
	}

	public static final String TOPIC_TIMER_EXCEPTION_EVENT_OCCURRED = TimerWrapperProviderJvm.class
			.getName() + "." + "TOPIC_TIMER_EXCEPTION_EVENT_OCCURRED";

	public static Topic<Throwable> topicWrapperException() {
		return Topic.global(TOPIC_TIMER_EXCEPTION_EVENT_OCCURRED);
	}

	public class TimerWrapperJvm implements TimerWrapper {
		private TimerTask task;

		private TimerWrapperJvm(final Runnable runnable) {
			task = new TimerTask() {
				@Override
				public void run() {
					try {
						runnable.run();
					} catch (Throwable e) {
						e.printStackTrace();
						topicWrapperException().publish(e);
					}
				}
			};
		}

		@Override
		public void cancel() {
			task.cancel();
		}

		@Override
		public void scheduleRepeating(long periodMillis) {
			timer.scheduleAtFixedRate(task, periodMillis, periodMillis);
		}

		@Override
		public void scheduleSingle(long delayMillis) {
			timer.schedule(task, delayMillis);
		}
	}
}