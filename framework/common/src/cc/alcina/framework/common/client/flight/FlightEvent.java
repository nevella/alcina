package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.process.GlobalObservable;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializationExceptionData;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.HandlesDeserializationException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;

public class FlightEvent extends Bindable.Fields
		implements GlobalObservable, HandlesDeserializationException,
		HasStringRepresentation, IdOrdered<FlightEvent>, HasId {
	public FlightEventWrappable event;

	public long id;

	public long time;

	public transient DeserializationExceptionData deserializationExceptionData;

	public FlightEvent() {
	}

	public FlightEvent(FlightEventWrappable event) {
		this.event = event;
		this.id = ProcessObservable.Id.nextId();
		this.time = System.currentTimeMillis();
	}

	public long getId() {
		return id;
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

	public String provideSubtype() {
		return event == null ? null : event.provideSubcategory();
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

	@Override
	public void setId(long id) {
		set("id", this.id, id, () -> this.id = id);
	}
}
