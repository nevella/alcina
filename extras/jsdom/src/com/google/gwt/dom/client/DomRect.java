package com.google.gwt.dom.client;

import java.io.Serializable;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.DoublePair;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * Note - it's really rare that x!=left, y!=top - but they differ when
 * width/height are negative
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public final class DomRect implements Serializable {
	public static DomRect fromOrigin(double width, double height) {
		DomRect result = new DomRect();
		result.width = width;
		result.height = height;
		result.computeDerived();
		return result;
	}

	/*
	 * see https://developer.mozilla.org/en-US/docs/Web/API/DOMRect
	 */
	void computeDerived() {
		top = height >= 0 ? y : y + height;
		bottom = height >= 0 ? y + height : y;
		left = width >= 0 ? x : x + width;
		right = width >= 0 ? x + width : x;
	}

	public static DomRect ofCoordinatePairs(double x1, double y1, double x2,
			double y2) {
		DomRect result = new DomRect();
		result.x = x1;
		result.width = x2 - x1;
		result.y = y1;
		result.height = y2 - y1;
		result.computeDerived();
		return result;
	}

	public double top;

	public double bottom;

	public double left;

	public double right;

	public double height;

	public double width;

	public double x;

	public double y;

	public DomRect() {
	}

	@Override
	public int hashCode() {
		return Objects.hash(top, left, bottom, right);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DomRect) {
			DomRect o = (DomRect) obj;
			return top == o.top && left == o.left && bottom == o.bottom
					&& right == o.right;
		} else {
			return super.equals(obj);
		}
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

	public DoublePair xRange() {
		return new DoublePair(left, right);
	}

	public DoublePair yRange() {
		return new DoublePair(top, bottom);
	}

	@Override
	public String toString() {
		return Ax.format("[%s,%s],[%s,%s]", left, top, right, bottom);
	}

	public boolean isZeroDimensions() {
		return left == 0 && top == 0 && right == 0 && bottom == 0;
	}

	public void computeFromTLBR() {
		x = left;
		y = top;
		width = right - left;
		height = bottom - top;
	}

	public boolean intersectsWith(DomRect other) {
		return xRange().intersectsWith(other.xRange())
				&& yRange().intersectsWith(other.yRange());
	}

	public DomRect toRoundedIntRect() {
		DomRect rounded = new DomRect();
		rounded.top = Math.round(top);
		rounded.bottom = Math.round(bottom);
		rounded.left = Math.round(left);
		rounded.right = Math.round(right);
		rounded.computeFromTLBR();
		return rounded;
	}

	public DomRect translate(IntPair offsets) {
		return ofCoordinatePairs(x + offsets.i1, y + offsets.i2,
				x + offsets.i1 + width, y + offsets.i2 + height);
	}
}
