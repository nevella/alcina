package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializationExceptionData;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.HandlesDeserializationException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.IdCounter;

@Bean(PropertySource.FIELDS)
public class FlightEvent implements ProcessObservable, Comparable<FlightEvent>,
		HandlesDeserializationException, HasStringRepresentation {
	static IdCounter counter = new IdCounter();

	public FlightEventWrappable event;

	public long eventId;

	public long time;

	public transient DeserializationExceptionData deserializationExceptionData;

	public FlightEvent() {
	}

	public FlightEvent(FlightEventWrappable event) {
		this.event = event;
		this.eventId = counter.nextId();
		this.time = System.currentTimeMillis();
	}

	@Override
	public int compareTo(FlightEvent o) {
		return CommonUtils.compareLongs(eventId, o.eventId);
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", eventId, event);
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
