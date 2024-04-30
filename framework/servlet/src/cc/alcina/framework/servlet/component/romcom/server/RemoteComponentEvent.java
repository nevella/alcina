package cc.alcina.framework.servlet.component.romcom.server;

import cc.alcina.framework.common.client.flight.HasSessionId;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/**
 * A request/response pair for flightrecording
 */
@Bean(PropertySource.FIELDS)
public class RemoteComponentEvent implements ProcessObservable, HasSessionId {
	public RemoteComponentEvent() {
	}

	public RemoteComponentEvent(RemoteComponentRequest request,
			RemoteComponentResponse response) {
		this.request = request;
		this.response = response;
	}

	public RemoteComponentRequest request;

	public RemoteComponentResponse response;

	@Property.Not
	@Override
	public String getSessionId() {
		return request.session.id;
	}
}
