package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IdCounter;

@Bean(PropertySource.FIELDS)
public class FlightEvent implements ProcessObservable, Comparable<FlightEvent> {
	static IdCounter counter = new IdCounter();

	public HasSessionId event;

	public long eventId;

	public FlightEvent() {
	}

	public FlightEvent(HasSessionId event) {
		this.event = event;
		this.eventId = counter.nextId();
	}

	@Override
	public int compareTo(FlightEvent o) {
		return CommonUtils.compareLongs(eventId, o.eventId);
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", eventId, event);
	}
}
