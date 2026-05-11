package cc.alcina.framework.servlet.component.console.rcs;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.gwt.client.dirndl.model.HasClassNames;

@TypedProperties
public class RomcomSessionEntry extends Bindable.Fields
		implements TreeSerializable, Comparable<RomcomSessionEntry>,
		HasClassNames, HasStringRepresentation {
	transient static PackageProperties._RomcomSessionEntry properties = PackageProperties.romcomSessionEntry;

	Date start;

	Date end;

	boolean active;

	boolean marked;

	boolean exception;

	int largestPacket;

	int slowestResponse;

	@Property.Not
	List<FlightEvent> events;

	String sessionId;

	String path;

	RomcomSessionEntry() {
	}

	public RomcomSessionEntry(List<FlightEvent> events, String sessionId,
			boolean active, String path) {
		this.events = events;
		this.sessionId = sessionId;
		this.path = path;
		start = new Date(Ax.first(events).time);
		end = active ? null : new Date(Ax.last(events).time);
		active = end == null;
		largestPacket = events.stream()
				.map(e -> e.event.provideOutputBytes().length)
				.max(Integer::compare).orElse(0);
	}

	@Override
	public String provideStringRepresentation() {
		return "no-rep";
	}

	@Override
	public List<String> provideClassNames() {
		return List.of();
	}

	@Override
	public int compareTo(RomcomSessionEntry o) {
		return start.compareTo(o.start);
	}
}
