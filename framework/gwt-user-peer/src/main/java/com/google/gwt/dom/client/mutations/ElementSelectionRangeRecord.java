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
 * A wire representation of the focussed input/textarea selectionrange
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public final class ElementSelectionRangeRecord {
	public AttachId nodeId;

	public int selectionStart;

	public int selectionEnd;

	public transient Node node;

	public void populateNodes() {
		if (nodeId != null) {
			node = nodeId.node();
		}
	}

	public void populateNodeIds() {
		nodeId = node == null ? null : AttachId.forNode(node);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, selectionStart, selectionEnd);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ElementSelectionRangeRecord) {
			ElementSelectionRangeRecord o = (ElementSelectionRangeRecord) obj;
			populateNodeIds();
			o.populateNodeIds();
			return CommonUtils.equals(nodeId, o.nodeId, selectionStart,
					o.selectionStart, selectionEnd, o.selectionEnd);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("nodeId", nodeId, "selectionStart",
				selectionStart, "selectionEnd", selectionEnd);
	}

	public ElementSelectionRangeRecord copy() {
		ElementSelectionRangeRecord copy = new ElementSelectionRangeRecord();
		copy.nodeId = nodeId;
		copy.selectionStart = selectionStart;
		copy.selectionEnd = selectionEnd;
		return copy;
	}

	@Property.Not
	public boolean isCollapsed() {
		return selectionStart == selectionEnd;
	}

	public Object toNodeString() {
		populateNodes();
		return FormatBuilder.keyValues("nodeId", nodeId, "selectionStart",
				selectionStart, "selectionEnd", selectionEnd);
	}
}
