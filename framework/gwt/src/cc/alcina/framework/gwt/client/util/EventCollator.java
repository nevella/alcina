package cc.alcina.framework.gwt.client.util;

import java.util.function.Consumer;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;

/*
 * Collects events and triggers an action after a specified delay
 */
public class EventCollator<T> {
	private long lastEventOccurred = 0;

	private long firstEventOccurred = 0;

	private T firstObject;

	private T lastObject;

	private Runnable checkCallback = new Runnable() {
		@Override
		public void run() {
			long time = System.currentTimeMillis();
			if (time - lastEventOccurred >= waitToPerformAction
					|| (maxDelayFromFirstEvent != 0 && (time
							- firstEventOccurred >= maxDelayFromFirstEvent))) {
				synchronized (this) {
					if (timer != null) {
						timer.cancel();
						timer = null;
					}
					firstEventOccurred = 0;
				}
				try {
					action.accept(EventCollator.this);
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

	private final Consumer<EventCollator<T>> action;

	private final TimerWrapperProvider timerWrapperProvider;

	private long maxDelayFromFirstEvent;

	private TimerWrapper timer = null;

	public EventCollator(long waitToPerformAction, Runnable runnable) {
		this(waitToPerformAction, collator -> runnable.run(),
				Registry.impl(TimerWrapperProvider.class));
	}

	public EventCollator(long waitToPerformAction,
			Consumer<EventCollator<T>> action) {
		this(waitToPerformAction, action,
				Registry.impl(TimerWrapperProvider.class));
	}

	public EventCollator(long waitToPerformAction,
			Consumer<EventCollator<T>> action,
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

	public void eventOccurred() {
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

	public void eventOccurred(T object) {
		synchronized (this) {
			if (firstObject == null) {
				firstObject = object;
			}
			lastObject = object;
		}
		eventOccurred();
	}

	public EventCollator
			withMaxDelayFromFirstEvent(long maxDelayFromFirstEvent) {
		this.maxDelayFromFirstEvent = maxDelayFromFirstEvent;
		return this;
	}
}
