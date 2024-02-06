package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

/**
 * Client/server-side event pump support. Initially not thread-safe (assumes
 * single-threaded), which is probably fine
 */
public class Events {
	public EmissionStyle emissionStyle;

	List<Runnable> events = new ArrayList<>();

	public Events() {
		emissionStyle = GWT.isClient() ? EmissionStyle.SCHEDULER
				: EmissionStyle.NONE;
	}

	public void pump() {
		Preconditions.checkState(emissionStyle == EmissionStyle.MANUAL);
		while (this.events.size() > 0) {
			List<Runnable> passEvents = this.events;
			this.events = new ArrayList<>();
			passEvents.forEach(Runnable::run);
		}
	}

	public void queue(Runnable event) {
		switch (emissionStyle) {
		case MANUAL:
			events.add(event);
			break;
		case NONE:
			break;
		case SCHEDULER:
			Scheduler.get().scheduleFinally(() -> event.run());
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public enum EmissionStyle {
		NONE,
		// events are placed on the scheduler's finally queue
		SCHEDULER,
		// events must be manually pumped
		MANUAL
	}
}