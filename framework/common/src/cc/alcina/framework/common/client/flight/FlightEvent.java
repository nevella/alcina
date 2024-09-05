package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializationExceptionData;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.HandlesDeserializationException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;

@Bean(PropertySource.FIELDS)
public class FlightEvent
		implements ProcessObservable, HandlesDeserializationException,
		HasStringRepresentation, IdOrdered<FlightEvent> {
	public FlightEventWrappable event;

	public long id;

	public long getId() {
		return id;
	}

	public long time;

	public transient DeserializationExceptionData deserializationExceptionData;

	public FlightEvent() {
	}

	public FlightEvent(FlightEventWrappable event) {
		this.event = event;
		this.id = ProcessObservable.Id.nextId();
		this.time = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", id, event);
	}

	@Override
	public void handleDeserializationException(
			DeserializationExceptionData deserializationExceptionData) {
		this.deserializationExceptionData = deserializationExceptionData;
	}

	public long provideDuration() {
		return event == null ? 0 : event.provideDuration();
	}

	public long provideTime() {
		return event == null || event.provideStart() == 0 ? time
				: event.provideStart();
	}

	public String provideDetail() {
		return event == null ? null : event.provideDetail();
	}

	public byte[] provideInputBytes() {
		return event == null ? new byte[0] : event.provideInputBytes();
	}

	public byte[] provideOutputBytes() {
		return event == null ? new byte[0] : event.provideOutputBytes();
	}

	@Override
	public String provideStringRepresentation() {
		return event == null ? null : event.provideStringRepresentation();
	}
}
