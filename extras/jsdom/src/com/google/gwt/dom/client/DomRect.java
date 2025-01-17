package com.google.gwt.dom.client;

import java.io.Serializable;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;

/**
 * Note - it's really rare that x!=left, y!=top - but they differ when
 * width/height are negative
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public final class DomRect implements Serializable {
	public double bottom;

	public double height;

	public double left;

	public double right;

	public double top;

	public double width;

	public double x;

	public double y;

	public DomRect() {
	}

	public DomRect(DomRectJso jso) {
		bottom = jso.getBottom();
		height = jso.getHeight();
		left = jso.getLeft();
		right = jso.getRight();
		top = jso.getTop();
		width = jso.getWidth();
		x = jso.getX();
		y = jso.getY();
	}
}
