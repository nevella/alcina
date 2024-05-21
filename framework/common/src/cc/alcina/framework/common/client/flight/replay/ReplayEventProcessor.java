package cc.alcina.framework.common.client.flight.replay;

import cc.alcina.framework.common.client.flight.FlightEvent;

public interface ReplayEventProcessor {
	public interface EmissionFilter {
	}

	public void replay(FlightEvent next);
}
