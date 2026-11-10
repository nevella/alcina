package cc.alcina.framework.common.client.flight.replay;

import cc.alcina.framework.common.client.flight.FlightEvent;

public interface ReplayEventProcessor {
	public interface EmissionFilter {
		boolean test(FlightEvent event);
	}

	EmissionFilter getEmissionFilter();

	default void replay(FlightEvent event) {
		if (getEmissionFilter().test(event)) {
			doReplay(event);
		}
	}

	void doReplay(FlightEvent event);
}
