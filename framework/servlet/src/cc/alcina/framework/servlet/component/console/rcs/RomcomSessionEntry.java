package cc.alcina.framework.servlet.component.console.rcs;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.gwt.client.dirndl.model.HasClassNames;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.AwaitTimedOutException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentEvent;

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

	long folderLastModificationDate;

	@Property.Not
	@AlcinaTransient
	List<FlightEvent> events;

	String sessionId;

	String path;

	RomcomSessionEntry() {
	}

	public RomcomSessionEntry(List<FlightEvent> events, String sessionId,
			boolean active, String path, long folderLastModificationDate) {
		this.events = events;
		this.sessionId = sessionId;
		this.path = path;
		start = new Date(Ax.first(events).time);
		end = active ? null : new Date(Ax.last(events).time);
		active = end == null;
		this.folderLastModificationDate = folderLastModificationDate;
		largestPacket = events.stream()
				.map(e -> e.event.provideOutputBytes().length)
				.max(Integer::compare).orElse(0);
		slowestResponse = events.stream().map(FlightEvent::provideDuration)
				.max(Long::compare).orElse(0L).intValue();
		exception = events.stream().anyMatch(this::isException);
	}

	boolean isException(FlightEvent event) {
		if (event.event instanceof RemoteComponentEvent) {
			RemoteComponentEvent rce = (RemoteComponentEvent) event.event;
			return rce.allMessages().anyMatch(m -> {
				if (m instanceof Message.IsException) {
					ProcessingException exception = (ProcessingException) m;
					if (Objects.equals(exception.exceptionClassName,
							InvalidClientException.class.getName())) {
						return false;
					} else if (Objects.equals(exception.exceptionClassName,
							AwaitTimedOutException.class.getName())) {
						return false;
					} else {
						return true;
					}
				}
				return false;
			});
		} else {
			return false;
		}
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

	public void persist() {
		RomcomSessionProvider.get().persist(this);
	}
}
