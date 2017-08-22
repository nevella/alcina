/**
 * 
 */
package cc.alcina.framework.common.client.util;

import java.util.List;
import java.util.stream.Collectors;

public class DoublePair implements Comparable<DoublePair> {
	public double d1;

	public double d2;

	public DoublePair() {
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DoublePair) {
			DoublePair dp = (DoublePair) obj;
			return d1 == dp.d1 && d2 == dp.d2;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new Double(d1).hashCode() ^ new Double(d2).hashCode();
	}

	public DoublePair(double i1, double i2) {
		this.d1 = i1;
		this.d2 = i2;
	}

	public void add(DoublePair dp) {
		d1 += dp.d1;
		d2 += dp.d2;
	}

	public void subtract(DoublePair dp) {
		d1 -= dp.d1;
		d2 -= dp.d2;
	}

	public void max(DoublePair dp) {
		d1 = d1 == 0 ? dp.d1 : Math.min(d1, dp.d1);
		d2 = d2 == 0 ? dp.d2 : Math.max(d1, dp.d2);
	}

	public void expand(double value) {
		d1 = d1 == 0 ? value : Math.min(d1, value);
		d2 = d2 == 0 ? value : Math.max(d2, value);
	}

	public int compareTo(DoublePair dp) {
		return d1 < dp.d1 ? -1
				: d1 > dp.d1 ? 1 : d2 < dp.d2 ? -1 : d2 > dp.d2 ? 1 : 0;
	}

	@Override
	public String toString() {
		return "[" + d1 + "," + d2 + "]";
	}

	public String toStringComma() {
		return d1 + "," + d2;
	}

	public static String coordinateString(List<DoublePair> pairs) {
		return pairs.stream().map(DoublePair::toStringComma)
				.collect(Collectors.joining(" "));
	}

	public boolean isZero() {
		return d1 == 0 && d2 == 0;
	}

	// top exclusive
	public boolean contains(double d) {
		return d1 == d2 ? d1 == d : ordered().d1 <= d && ordered().d2 > d;
	}

	public double average() {
		return (d1 + d2) / 2;
	}

	public DoublePair ordered() {
		if (d1 <= d2) {
			return this;
		}
		return new DoublePair(d2, d1);
	}

	public DoublePair intersection(DoublePair other) {
		DoublePair o1 = ordered();
		DoublePair o2 = other.ordered();
		DoublePair result = new DoublePair();
		result.d1 = Math.max(o1.d1, o2.d1);
		result.d2 = Math.min(o1.d2, o2.d2);
		return result.d1 <= result.d2 ? result : null;
	}

	public boolean intersectsWith(DoublePair other) {
		return intersection(other) != null;
	}

	public double distance() {
		return Math.abs(d1 - d2);
	}

	public double overlap(DoublePair fp2) {
		DoublePair intersection = intersection(fp2);
		if (intersection == null) {
			return 0.0F;
		}
		return intersection.distance() * 2 / (distance() + fp2.distance());
	}
}