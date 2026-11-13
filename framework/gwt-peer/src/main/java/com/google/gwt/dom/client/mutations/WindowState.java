package com.google.gwt.dom.client.mutations;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;

/**
 * A wire representation of the curreent DOM window state
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public final class WindowState {
	public int focusOffset;

	@Override
	public int hashCode() {
		return Objects.hash(focusOffset);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WindowState) {
			WindowState o = (WindowState) obj;
			return CommonUtils.equals(focusOffset, o.focusOffset);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("focusOffset", focusOffset);
	}
}
