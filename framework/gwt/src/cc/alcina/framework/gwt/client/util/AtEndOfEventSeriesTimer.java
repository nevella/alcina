package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;

public class AtEndOfEventSeriesTimer {
	private long lastEventOccurred = 0;

	private long firstEventOccurred = 0;

	private Runnable checkCallback = new Runnable() {
		@Override
		public void run() {
			long time = System.currentTimeMillis();
			if (time - lastEventOccurred >= waitToPerformAction
					|| (maxDelayFromFirstAction != 0 && (time
							- firstEventOccurred >= maxDelayFromFirstAction))) {
				synchronized (this) {
					if (timer != null) {
						timer.cancel();
						timer = null;
					}
					firstEventOccurred = 0;
				}
				action.run();
			}
		}
	};

	private final long waitToPerformAction;

	private final Runnable action;

	private final TimerWrapperProvider timerWrapperProvider;

	private long maxDelayFromFirstAction;

	public AtEndOfEventSeriesTimer(long waitToPerformAction, Runnable action) {
		this(waitToPerformAction, action, Registry
				.impl(TimerWrapperProvider.class));
	}

	public AtEndOfEventSeriesTimer(long waitToPerformAction, Runnable action,
			TimerWrapperProvider timerWrapperProvider) {
		this.waitToPerformAction = waitToPerformAction;
		this.action = action;
		this.timerWrapperProvider = timerWrapperProvider;
	}

	public AtEndOfEventSeriesTimer maxDelayFromFirstAction(
			long maxDelayFromFirstAction) {
		this.maxDelayFromFirstAction = maxDelayFromFirstAction;
		return this;
	}

	private TimerWrapper timer = null;

	public void triggerEventOccurred() {
		synchronized (this) {
			lastEventOccurred = System.currentTimeMillis();
			if (firstEventOccurred == 0) {
				firstEventOccurred = lastEventOccurred;
			}
			if (timer == null && timerWrapperProvider != null) {
				timer = timerWrapperProvider.getTimer(checkCallback);
				timer.scheduleRepeating(waitToPerformAction / 2);
			}
		}
	}

	public void cancel() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
}
