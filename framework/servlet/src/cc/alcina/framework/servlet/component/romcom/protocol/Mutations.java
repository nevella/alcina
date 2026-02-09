package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.SelectionRecord;

import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageId;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.HasTimeline;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.PrependWindowState;

/*
 * An album by Beck. Amazing.
 */
public class Mutations extends RemoteComponentProtocol.Message
		implements PrependWindowState, HasTimeline {
	public static class Rejected implements ContextObservable {
		public Mutations mutations;

		public Rejected(Mutations mutations) {
			this.mutations = mutations;
		}
	}

	public static Mutations ofLocation() {
		Mutations result = new Mutations();
		result.locationMutation = LocationMutation.ofWindow(false);
		return result;
	}

	public MessageId counterpartProcessingId;

	// TODO - romcom/ref.ser, serialized, there should be no classname
	// (but there is)
	/*
	 * wip - localdom
	 */
	public List<MutationRecord> domMutations = new ArrayList<>();

	public List<EventSystemMutation> eventSystemMutations = new ArrayList<>();

	public LocationMutation locationMutation;

	public SelectionRecord selectionMutation;

	@Property.Not
	public MessageId getCounterpartProcessingId() {
		return counterpartProcessingId;
	}

	@Override
	public String toDebugString() {
		return FormatBuilder.keyValues("dom", domMutations.size(), "event",
				eventSystemMutations.size(), "loc", locationMutation);
	}

	public void addDomMutation(MutationRecord mutationRecord) {
		domMutations.add(mutationRecord);
	}

	@Property.Not
	@Override
	public void setCounterpartProcessingId(MessageId counterpartProcessingId) {
		this.counterpartProcessingId = counterpartProcessingId;
	}

	@Override
	protected String provideMessageData() {
		FormatBuilder format = new FormatBuilder().separator(" - ");
		if (domMutations.size() > 0) {
			format.append(domMutations.stream().map(dm -> dm.target.nodeName)
					.distinct().collect(Collectors.joining(", ")));
		}
		if (domMutations.isEmpty() && eventSystemMutations.size() > 0) {
			format.append(
					eventSystemMutations.stream().map(esm -> esm.eventTypeName)
							.distinct().collect(Collectors.joining(", ")));
		}
		return format.toString();
	}
}