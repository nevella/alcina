package cc.alcina.framework.common.client.flight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;

/**
 * A simple memory store of events (for client flight)
 */
public class FlightEvents implements ProcessObserver<FlightEvent> {
	List<FlightEvent> events = Collections.synchronizedList(new ArrayList<>());

	public void observe() {
		ProcessObservers.observe(this, true);
	}

	@Override
	public void topicPublished(FlightEvent message) {
		events.add(message);
	}
}
