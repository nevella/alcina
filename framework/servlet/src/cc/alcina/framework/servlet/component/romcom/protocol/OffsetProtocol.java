package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.WindowState.NodeUiState;
import com.google.gwt.dom.client.WindowState.OffsetsDelta;
import com.google.gwt.dom.client.WindowState.OffsetsDelta.ElementOffsets;
import com.google.gwt.dom.client.behavior.ElementOffsetsRequired;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;

/**
 * <p>
 * The biggest client-to-server communication cost is transmitting the offset
 * state of nodes where the server has registered a need-to-know (
 * {@link ElementOffsetsRequired} behaviour)
 * 
 * <p>
 * This class optimises by only transmitting changes, and providing a
 * server-side view of current client state generated from the delta sequence
 */
@Feature.Ref(Feature_Romcom_Impl._OffsetProtocol.class)
public class OffsetProtocol {
	/**
	 * The registry of offsets synced via the protocol
	 */
	public static class OffsetRegistry {
		Map<AttachId, ElementOffsets> attachIdOffsets = AlcinaCollections
				.newLinkedHashMap();

		Map<AttachId, ElementOffsets> descendantOfRelativeFixedOffsets = AlcinaCollections
				.newLinkedHashMap();

		/*
		 * null value means no relativeFixed ancestor
		 */
		Map<AttachId, AttachId> descendantToRelativeFixed = AlcinaCollections
				.newLinkedHashMap();

		/*
		 * inverse of descendantToRelativeFixed
		 */
		Multimap<AttachId, List<AttachId>> relativeFixedDescendant = new Multimap<>();

		Set<AttachId> lastObserved = AlcinaCollections.newHashSet();

		public ElementOffsets getOffsetsWithInvariant(Node node) {
			AttachId attachId = AttachId.forNode(node);
			ElementOffsets invariant = descendantOfRelativeFixedOffsets
					.get(attachId);
			if (invariant != null) {
				return invariant;
			}
			ElementOffsets computed = ElementOffsets.of(node);
			attachIdOffsets.put(attachId, computed);
			if (node instanceof Element) {
				List<AttachId> ascent = new ArrayList<>();
				DomNode cursor = node.asDomNode();
				boolean fixed = false;
				AttachId descendantRelativeFixedAncestorId = null;
				while (cursor.isElement()) {
					if (cursor.hasBehavior(
							ElementOffsetsRequired.DescendantRelativeFixed.class)) {
						descendantRelativeFixedAncestorId = cursor.attachId();
						fixed = true;
						break;
					}
					AttachId cursorId = cursor.attachId();
					if (descendantToRelativeFixed.containsKey(cursorId)) {
						descendantRelativeFixedAncestorId = descendantToRelativeFixed
								.get(cursorId);
						fixed = descendantRelativeFixedAncestorId != null;
						break;
					}
					ascent.add(cursor.attachId());
					cursor = cursor.parent();
				}
				if (!node.asDomNode().hasBehavior(
						ElementOffsetsRequired.DescendantRelativeFixed.class)) {
					if (fixed) {
						descendantOfRelativeFixedOffsets.put(attachId,
								computed);
					}
					for (AttachId ascentId : ascent) {
						descendantToRelativeFixed.put(ascentId,
								descendantRelativeFixedAncestorId);
						if (descendantRelativeFixedAncestorId != null) {
							relativeFixedDescendant.add(
									descendantRelativeFixedAncestorId,
									ascentId);
						}
					}
				}
			}
			return computed;
		}

		public OffsetsDelta
				computeOffsetsDelta(Set<Element> observedElementTree) {
			OffsetsDelta result = new OffsetsDelta();
			Set<AttachId> removed = lastObserved;
			Set<AttachId> observedIds = AlcinaCollections.newHashSet();
			result.changes = new ArrayList<>();
			invalidateRemovedDescendantInvariants();
			observedElementTree.forEach(elem -> {
				AttachId observedId = AttachId.forNode(elem);
				observedIds.add(observedId);
				ElementOffsets existing = attachIdOffsets.get(observedId);
				ElementOffsets offsets = getOffsetsWithInvariant(elem);
				if (existing != null) {
					removed.remove(observedId);
					if (Objects.equals(existing, offsets)) {
						return;// no change
					}
				}
				result.changes.add(offsets);
			});
			attachIdOffsets.keySet().removeAll(removed);
			descendantOfRelativeFixedOffsets.keySet().removeAll(removed);
			descendantToRelativeFixed.keySet().removeAll(removed);
			result.removed = removed;
			lastObserved = observedIds;
			return result;
		}

		/*
		 * note this doesn't handle invalidation of NON-descendant fixed, but
		 * that's just a performance, not logic issue
		 */
		void invalidateRemovedDescendantInvariants() {
			List<AttachId> removed = relativeFixedDescendant.keySet().stream()
					.filter(e -> e.isDetached() || e.node() == null
							|| !((Element) e.node()).hasBehavior(
									ElementOffsetsRequired.DescendantRelativeFixed.class))
					.toList();
			removed.forEach(removedId -> {
				List<AttachId> descendants = relativeFixedDescendant
						.remove(removedId);
				relativeFixedDescendant.remove(removedId);
				descendants.forEach(descId -> {
					descendantOfRelativeFixedOffsets.remove(descId);
					descendantToRelativeFixed.remove(descId);
				});
			});
		}

		public void update(OffsetsDelta offsetsDelta) {
			attachIdOffsets.keySet().removeAll(offsetsDelta.removed);
			offsetsDelta.changes.forEach(change -> {
				attachIdOffsets.put(change.id, change);
			});
		}

		public boolean containsKey(AttachId attachId) {
			return attachIdOffsets.containsKey(attachId);
		}

		public NodeUiState computeNodeUiState(AttachId attachId) {
			NodeUiState computed = new NodeUiState();
			computed.nodeId = attachId;
			computed.boundingClientRect = new DomRect();
			ElementOffsets offsets = attachIdOffsets.get(attachId);
			computed.scrollPos = IntPair.of(offsets.scrollLeft,
					offsets.scrollTop);
			Node node = offsets.id.node();
			if (node.getOwnerDocument().getDocumentElement() == node) {
				/*
				 * short-circuit
				 */
				computed.boundingClientRect.top = computed.scrollPos.i2;
				computed.boundingClientRect.left = computed.scrollPos.i1;
				computed.boundingClientRect.right = computed.boundingClientRect.left
						+ offsets.offsetWidth;
				computed.boundingClientRect.bottom = computed.boundingClientRect.top
						+ offsets.offsetHeight;
			} else {
				/*
				 * see com.google.gwt.dom.client.DOMImpl.getSubPixelAbsoluteTop(
				 * Element multiplex)
				 */
				{
					ElementOffsets cursor = offsets;
					int top = 0;
					// This intentionally excludes body which has a null
					// offsetParent.
					while (cursor.offsetParentId != null) {
						top -= cursor.scrollTop;
						cursor = attachIdOffsets.get(cursor.offsetParentId);
					}
					cursor = offsets;
					while (cursor != null) {
						top += cursor.offsetTop;
						cursor = attachIdOffsets.get(cursor.offsetParentId);
					}
					computed.absoluteTop = top;
				}
				{
					ElementOffsets cursor = offsets;
					int left = 0;
					// This intentionally excludes body which has a null
					// offsetParent.
					while (cursor.offsetParentId != null) {
						left -= cursor.scrollLeft;
						cursor = attachIdOffsets.get(cursor.offsetParentId);
					}
					cursor = offsets;
					while (cursor != null) {
						left += cursor.offsetLeft;
						cursor = attachIdOffsets.get(cursor.offsetParentId);
					}
					computed.absoluteLeft = left;
				}
				computed.boundingClientRect.top = computed.absoluteTop
						- Window.getScrollTop();
				computed.boundingClientRect.left = computed.absoluteLeft
						- Window.getScrollLeft();
				computed.boundingClientRect.right = computed.boundingClientRect.left
						+ offsets.offsetWidth;
				computed.boundingClientRect.bottom = computed.boundingClientRect.top
						+ offsets.offsetHeight;
			}
			computed.boundingClientRect.computeFromTLBR();
			return computed;
		}
	}
}
