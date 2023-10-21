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

	public Events() {
		emissionStyle = GWT.isClient() ? EmissionStyle.SCHEDULER
				: EmissionStyle.NONE;
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

	public void pump() {
		Preconditions.checkState(emissionStyle == EmissionStyle.MANUAL);
		events.forEach(Runnable::run);
	}

	List<Runnable> events = new ArrayList<>();

	public enum EmissionStyle {
		NONE,
		// events are placed on the scheduler's finally queue
		SCHEDULER,
		// events must be manually pumped
		MANUAL
	}
}