package com.google.gwt.dom.client;

import cc.alcina.framework.common.client.csobjects.Bindable;

public class DomRect extends Bindable.Fields {
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
