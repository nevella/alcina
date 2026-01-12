package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.dom.client.AttachId;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteElementBehaviors;

/**
 * The biggest client-server communication cost is transmitting the offset state
 * of nodes where the server has registered a need-to-know (
 * {@link RemoteElementBehaviors.ElementOffsetsRequired} behaviour)
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
			result.removed = AlcinaCollections.newHashSet();
			result.removed.addAll(attachIdOffsets.keySet());
			result.changes = new ArrayList<>();
			currentOffsets.forEach(offsets -> {
				ElementOffsets existing = attachIdOffsets.get(offsets.id);
				if (existing != null) {
					result.removed.remove(offsets.id);
					if (Objects.equals(existing, offsets)) {
						return;// no change
					}
					result.changes.add(offsets);
				}
			});
			return result;
		}
	}

	public static class OffsetsDelta {
		List<ElementOffsets> changes;

		Set<AttachId> removed;
	}

	/**
	 * The data synced via the protocol
	 */
	@Bean(PropertySource.FIELDS)
	public static class ElementOffsets {
		public AttachId id;

		public AttachId offsetParentId;

		public double offsetLeft;

		public double offsetWidth;

		public double offsetTop;

		public double offsetHeight;

		public double scrollLeft;

		public double scrollTop;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ElementOffsets) {
				ElementOffsets o = (ElementOffsets) obj;
				return Objects.equals(id, o.id) && Objects.equals(id, o.id)
						&& offsetHeight == o.offsetHeight
						&& offsetLeft == o.offsetLeft
						&& offsetTop == o.offsetTop
						&& offsetWidth == o.offsetWidth
						&& scrollLeft == o.scrollLeft
						&& scrollTop == o.scrollTop;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, offsetParentId, offsetLeft, offsetWidth,
					offsetTop, offsetHeight, scrollLeft, scrollTop);
		}
	}
}
