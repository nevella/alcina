package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.WindowState.OffsetsDelta;
import com.google.gwt.dom.client.WindowState.OffsetsDelta.ElementOffsets;
import com.google.gwt.dom.client.behavior.RemoteElementBehaviors;

import cc.alcina.framework.common.client.util.AlcinaCollections;

/**
 * <p>
 * The biggest client-to-server communication cost is transmitting the offset
 * state of nodes where the server has registered a need-to-know (
 * {@link RemoteElementBehaviors.ElementOffsetsRequired} behaviour)
 * 
 * <p>
 * This class optimises by only transmitting changes, and providing a
 * server-side view of current client state generated from the delta sequence
 */
public class OffsetProtocol {
	/**
	 * The registry of offsets synced via the protocol
	 */
	public static class OffsetRegistry {
		Map<AttachId, ElementOffsets> attachIdOffsets = AlcinaCollections
				.newLinkedHashMap();

		public OffsetsDelta
				computeOffsetsDelta(List<ElementOffsets> currentOffsets) {
			OffsetsDelta result = new OffsetsDelta();
			Set<AttachId> removed = AlcinaCollections.newHashSet();
			removed.addAll(attachIdOffsets.keySet());
			result.changes = new ArrayList<>();
			currentOffsets.forEach(offsets -> {
				ElementOffsets existing = attachIdOffsets.get(offsets.id);
				if (existing != null) {
					removed.remove(offsets.id);
					if (Objects.equals(existing, offsets)) {
						return;// no change
					}
				}
				result.changes.add(offsets);
				attachIdOffsets.put(offsets.id, offsets);
			});
			attachIdOffsets.keySet().removeAll(removed);
			result.removed = removed;
			return result;
		}
	}
}
