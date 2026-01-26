package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * <p>
 * An instance of this class is sent from the browser to the romcom server to
 * try and optimise (remove the need for most) queries from the server to the
 * dom.
 * 
 * <p>
 * Instances are sent when client DOM events occur, or when the DOM is changed.
 * 
 * <p>
 * It contains the non-dom browser objects most commonly needed by handling code
 * - focus, scroll position, selection etc
 * 
 * <p>
 * Data are sent for: [window, all ancestors inclusive of the active element,
 * elements with attribute -rc-transmit-state]
 */
@Bean(PropertySource.FIELDS)
public final class WindowState {
	/**
	 * The dimensions and scroll pos of a Node (Document corresonds to Window)
	 */
	@Bean(PropertySource.FIELDS)
	public final static class NodeUiState {
		public DomRect boundingClientRect;

		public IntPair scrollPos;

		public AttachId nodeId;

		public int absoluteTop;

		public int absoluteLeft;

		@Override
		public String toString() {
			return FormatBuilder.keyValues("boundingClientRect",
					boundingClientRect, "scrollPos", scrollPos, "element",
					nodeId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(boundingClientRect, scrollPos, nodeId);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NodeUiState) {
				NodeUiState o = (NodeUiState) obj;
				return CommonUtils.equals(boundingClientRect,
						o.boundingClientRect, scrollPos, o.scrollPos, nodeId,
						o.nodeId);
			} else {
				return super.equals(obj);
			}
		}
	}

	public AttachId activeElement;

	public List<NodeUiState> nodeUiStates = new ArrayList<>();

	public OffsetsDelta offsetsDelta;

	transient Map<Integer, NodeUiState> idUiState;

	public int clientHeight;

	public int clientWidth;

	public NodeUiState uiStateFor(int attachId) {
		if (idUiState == null) {
			idUiState = nodeUiStates.stream().collect(
					AlcinaCollectors.toKeyMap(state -> state.nodeId.id));
		}
		return idUiState.get(attachId);
	}

	@Bean(PropertySource.FIELDS)
	public final static class OffsetsDelta {
		public List<ElementOffsets> changes;

		public Set<AttachId> removed;

		/**
		 * The data synced via the protocol
		 */
		@Bean(PropertySource.FIELDS)
		public final static class ElementOffsets {
			public static ElementOffsets of(Node node) {
				ElementOffsets result = new ElementOffsets();
				result.id = AttachId.forNode(node);
				if (node instanceof Element) {
					Element elem = (Element) node;
					Element offsetParent = elem.getOffsetParent();
					result.offsetParentId = AttachId.forNode(offsetParent);
					result.offsetHeight = elem.getOffsetHeight();
					result.offsetLeft = elem.getOffsetLeft();
					result.offsetTop = elem.getOffsetTop();
					result.offsetWidth = elem.getOffsetWidth();
					result.scrollLeft = elem.getScrollLeft();
					result.scrollTop = elem.getScrollTop();
				}
				return result;
			}

			public AttachId id;

			public AttachId offsetParentId;

			/*
			 * these should really be doubles, but that requires a full gwt dom
			 * rework
			 */
			public int offsetLeft;

			public int offsetWidth;

			public int offsetTop;

			public int offsetHeight;

			public int scrollLeft;

			public int scrollTop;

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

		public String toSizes() {
			return Ax.format("changes: %s - removed: %s", changes.size(),
					removed.size());
		}
	}
}