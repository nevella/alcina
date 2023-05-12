package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;

public class AtEndOfEventSeriesTimer<T> {
	private long lastEventOccurred = 0;

	private long firstEventOccurred = 0;

	private T firstObject;

	private T lastObject;

	private Runnable checkCallback = new Runnable() {
		@Override
		public void run() {
			long time = System.currentTimeMillis();
			if (time - lastEventOccurred >= waitToPerformAction
					|| (maxDelayFromFirstAction != 0 && (time
							- firstEventOccurred >= maxDelayFromFirstAction))) {
				Ax.out("Debouncer: Firing: time: %s firstEvent : %s - lastEvent: %s",
						time, firstEventOccurred, lastEventOccurred);
				synchronized (this) {
					if (timer != null) {
						timer.cancel();
						timer = null;
					}
					firstEventOccurred = 0;
				}
				try {
					action.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				synchronized (this) {
					if (firstEventOccurred == 0) {
						firstObject = null;
						lastObject = null;
					}
				}
			}
		}
	};

	private final long waitToPerformAction;

	private final Runnable action;

	private final TimerWrapperProvider timerWrapperProvider;

	private long maxDelayFromFirstAction;

	private TimerWrapper timer = null;

	public AtEndOfEventSeriesTimer(long waitToPerformAction, Runnable action) {
		this(waitToPerformAction, action,
				Registry.impl(TimerWrapperProvider.class));
	}

	public AtEndOfEventSeriesTimer(long waitToPerformAction, Runnable action,
			TimerWrapperProvider timerWrapperProvider) {
		this.waitToPerformAction = waitToPerformAction;
		this.action = action;
		this.timerWrapperProvider = timerWrapperProvider;
	}

	public void cancel() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	public T getFirstObject() {
		return this.firstObject;
	}

	public T getLastObject() {
		return this.lastObject;
	}

	public AtEndOfEventSeriesTimer
			maxDelayFromFirstAction(long maxDelayFromFirstAction) {
		this.maxDelayFromFirstAction = maxDelayFromFirstAction;
		return this;
	}

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

	public void triggerEventOccurred(T object) {
		synchronized (this) {
			if (firstObject == null) {
				firstObject = object;
			}
			lastObject = object;
		}
		triggerEventOccurred();
	}
}
