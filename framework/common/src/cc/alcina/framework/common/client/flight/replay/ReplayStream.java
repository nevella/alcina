package cc.alcina.framework.common.client.flight.replay;

import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.flight.FlightEvent;

public class ReplayStream {
	public ReplayStream(List<FlightEvent> events) {
		this.events = events;
		itr = events.iterator();
	}

	List<FlightEvent> events;

	Iterator<FlightEvent> itr;
}
