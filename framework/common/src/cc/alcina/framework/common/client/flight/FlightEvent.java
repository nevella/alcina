package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.IdCounter;

@Bean(PropertySource.FIELDS)
public class FlightEvent implements ProcessObservable {
	static IdCounter counter = new IdCounter();

	public HasSessionId event;

	public long eventId;

	public FlightEvent() {
	}

	public FlightEvent(HasSessionId event) {
		this.event = event;
		this.eventId = counter.nextId();
	}
}
