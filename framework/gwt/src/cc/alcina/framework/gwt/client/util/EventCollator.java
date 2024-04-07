package cc.alcina.framework.gwt.client.util;

import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Timer;

/*
 * Collects events and triggers an action after a specified delay
 * 
 * Synchronization - all access to the timer should be synced on the Collator
 * 
 * Synchronization - all access to finished should be synced on finishedMonitor
 */
public class EventCollator<T> {
	private long lastEventOccurred = 0;

	private long firstEventOccurred = 0;

	private T firstObject;

	private T lastObject;

	private int collationActionsInvoked;

	private Runnable checkCallback = new Runnable() {
		@Override
		public void run() {
			long time = System.currentTimeMillis();
			long maxDelay = collationActionsInvoked == 0
					? maxDelayFromFirstEvent
					: maxDelayFromFirstCollatedEvent;
			boolean execute = time - lastEventOccurred >= waitToPerformAction
					|| (maxDelay != 0
							&& (time - firstEventOccurred >= maxDelay));
			if (!execute) {
				return;
			}
			synchronized (EventCollator.this) {
				if (timer != null) {
					timer.cancel();
					timer = null;
				}
				firstEventOccurred = 0;
				collationActionsInvoked++;
			}
			try {
				synchronized (finishedMonitor) {
					if (!finished) {
						action.accept(EventCollator.this);
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			synchronized (EventCollator.this) {
				if (firstEventOccurred == 0) {
					firstObject = null;
					lastObject = null;
				}
			}
		}
	};

	private final long waitToPerformAction;

	private final Consumer<EventCollator<T>> action;

	private final Timer.Provider timerProvider;

	// the delay before the (first) collation action call from the first event
	// received by this collator
	private long maxDelayFromFirstEvent;

	// the delay before the collation action call from the first event received
	// in the current collation. defaults to maxDelayFromFirstEvent
	private long maxDelayFromFirstCollatedEvent;

	private Timer timer = null;

	private boolean finished = false;

	private Object finishedMonitor = new Object();

	boolean runOnCurrentThread;

	public EventCollator(long waitToPerformAction,
			Consumer<EventCollator<T>> action) {
		this(waitToPerformAction, action, Registry.impl(Timer.Provider.class));
	}

	public EventCollator(long waitToPerformAction,
			Consumer<EventCollator<T>> action, Timer.Provider timerProvider) {
		this.waitToPerformAction = waitToPerformAction;
		this.action = action;
		this.timerProvider = timerProvider;
	}

	public EventCollator(long waitToPerformAction, Runnable runnable) {
		this(waitToPerformAction, collator -> runnable.run(),
				Registry.impl(Timer.Provider.class));
	}

	public void cancel() {
		synchronized (this) {
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
		}
		synchronized (finishedMonitor) {
			finished = true;
			if (firstEventOccurred != 0) {
				action.accept(this);
			}
		}
	}

	public void eventOccurred() {
		synchronized (this) {
			lastEventOccurred = System.currentTimeMillis();
			if (firstEventOccurred == 0) {
				firstEventOccurred = lastEventOccurred;
			}
			if (runOnCurrentThread) {
				checkCallback.run();
			} else {
				if (timer == null && timerProvider != null) {
					timer = timerProvider.getTimer(checkCallback);
					timer.scheduleRepeating(waitToPerformAction / 2);
				}
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

	public T getFirstObject() {
		return this.firstObject;
	}

	public T getLastObject() {
		return this.lastObject;
	}

	public EventCollator withMaxDelayFromFirstCollatedEvent(
			long maxDelayFromFirstCollatedEvent) {
		Preconditions.checkArgument(
				maxDelayFromFirstCollatedEvent >= maxDelayFromFirstEvent);
		this.maxDelayFromFirstCollatedEvent = maxDelayFromFirstCollatedEvent;
		return this;
	}

	public EventCollator
			withMaxDelayFromFirstEvent(long maxDelayFromFirstEvent) {
		this.maxDelayFromFirstEvent = maxDelayFromFirstEvent;
		if (this.maxDelayFromFirstCollatedEvent == 0) {
			this.maxDelayFromFirstCollatedEvent = maxDelayFromFirstEvent;
		}
		return this;
	}

	public EventCollator<T> withRunOnCurrentThread(boolean runOnCurrentThread) {
		this.runOnCurrentThread = runOnCurrentThread;
		return this;
	}
}
