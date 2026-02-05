package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.SelectionRecord;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.SendChannelId;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.PrependWindowState;

/*
 * An album by Beck. Amazing.
 */
public class Mutations extends RemoteComponentProtocol.Message
		implements PrependWindowState {
	@Bean(PropertySource.FIELDS)
	public static class MutationId implements Comparable<MutationId> {
		public static int nullAwareCompare(MutationId o1, MutationId o2) {
			return CommonUtils.compareWithNullMinusOne(o1, o2);
		}

		public SendChannelId sendChannelId;

		public int number;

		public MutationId(SendChannelId sendChannelId, int number) {
			this.sendChannelId = sendChannelId;
			this.number = number;
		}

		public MutationId() {
		}

		@Override
		public int compareTo(MutationId o) {
			Preconditions.checkState(sendChannelId == o.sendChannelId);
			return number - o.number;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MutationId) {
				MutationId o = (MutationId) obj;
				return sendChannelId == o.sendChannelId && number == o.number;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return sendChannelId.hashCode() ^ number;
		}

		@Override
		public String toString() {
			return Ax.format("#%s [%s]", number, sendChannelId);
		}
	}

	public MutationId mutationId;

	public MutationId highestVisibleCounterpartId;

	public static Mutations ofLocation() {
		Mutations result = new Mutations();
		result.locationMutation = LocationMutation.ofWindow(false);
		return result;
	}

	// TODO - romcom/ref.ser, serialized, there should be no classname
	// (but there is)
	public List<MutationRecord> domMutations = new ArrayList<>();

	public List<EventSystemMutation> eventSystemMutations = new ArrayList<>();

	public LocationMutation locationMutation;

	public SelectionRecord selectionMutation;

	@Override
	public String toDebugString() {
		return FormatBuilder.keyValues("dom", domMutations.size(), "event",
				eventSystemMutations.size(), "loc", locationMutation);
	}

	public void addDomMutation(MutationRecord mutationRecord) {
		domMutations.add(mutationRecord);
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