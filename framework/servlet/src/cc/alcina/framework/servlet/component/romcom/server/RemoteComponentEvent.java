package cc.alcina.framework.servlet.component.romcom.server;

import java.nio.charset.StandardCharsets;

import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/**
 * A request/response pair for flightrecording
 */
@Bean(PropertySource.FIELDS)
public class RemoteComponentEvent
		implements ProcessObservable, FlightEventWrappable {
	public long start;

	public long end;

	public RemoteComponentEvent() {
	}

	public RemoteComponentEvent(RemoteComponentRequest request,
			RemoteComponentResponse response, long start, long end) {
		this.request = request;
		this.response = response;
		this.start = start;
		this.end = end;
	}

	public RemoteComponentRequest request;

	public RemoteComponentResponse response;

	@Override
	public long provideStart() {
		return start;
	}

	@Override
	public long provideDuration() {
		return end - start;
	}

	@Property.Not
	@Override
	public String getSessionId() {
		return request.session.id;
	}

	@Override
	public String provideDetail() {
		return Ax.format("%s --> %s",
				NestedName.getSimple(request.protocolMessage),
				NestedName.getSimple(response.protocolMessage));
	}

	@Override
	public byte[] provideInputBytes() {
		return request == null || request.protocolMessage == null ? new byte[0]
				: FlightEventWrappable.serialize(request.protocolMessage)
						.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public byte[] provideOutputBytes() {
		return response == null || response.protocolMessage == null
				? new byte[0]
				: FlightEventWrappable.serialize(response.protocolMessage)
						.getBytes(StandardCharsets.UTF_8);
	}
}
