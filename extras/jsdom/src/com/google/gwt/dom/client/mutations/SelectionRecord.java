package com.google.gwt.dom.client.mutations;

import java.io.Serializable;
import java.util.Objects;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Node;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;

/**
 * A wire representation of the curreent DOM Selection state
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public final class SelectionRecord {
	public AttachId anchorNodeId;

	public int anchorOffset;

	public DomRect clientRect;

	public AttachId focusNodeId;

	public int focusOffset;

	public String type;

	public transient Node anchorNode;

	public transient Node focusNode;

	public void populateNodes() {
		if (anchorNodeId != null) {
			anchorNode = anchorNodeId.node();
		}
		if (focusNodeId != null) {
			focusNode = focusNodeId.node();
		}
	}

	public void populateNodeIds() {
		anchorNodeId = anchorNode == null ? null : AttachId.forNode(anchorNode);
		focusNodeId = focusNode == null ? null : AttachId.forNode(focusNode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(anchorNodeId, anchorOffset, focusNodeId,
				focusOffset, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SelectionRecord) {
			SelectionRecord o = (SelectionRecord) obj;
			return CommonUtils.equals(anchorNodeId, o.anchorNodeId,
					anchorOffset, o.anchorOffset, focusNodeId, o.focusNodeId,
					focusOffset, o.focusOffset, type, o.type, clientRect,
					o.clientRect);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("anchorNodeId", anchorNodeId,
				"anchorOffset", anchorOffset, "focusNodeId", focusNodeId,
				"focusOffset", focusOffset);
	}

	public SelectionRecord copy() {
		SelectionRecord copy = new SelectionRecord();
		copy.anchorNodeId = anchorNodeId;
		copy.anchorOffset = anchorOffset;
		copy.clientRect = clientRect;
		copy.focusNodeId = focusNodeId;
		copy.focusOffset = focusOffset;
		copy.type = type;
		return copy;
	}

	@Property.Not
	public boolean isCollapsed() {
		return CommonUtils.equals(anchorNodeId, focusNodeId, anchorOffset,
				focusOffset);
	}

	public Object toNodeString() {
		populateNodes();
		return FormatBuilder.keyValues("anchorNodeId", anchorNodeId,
				"anchorNode", anchorNode, "anchorOffset", anchorOffset,
				"focusNodeId", focusNodeId, "focusNode", focusNode,
				"focusOffset", focusOffset);
	}
}
