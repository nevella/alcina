/**
 */
package cc.alcina.framework.common.client.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

/**
 * <p>
 * Models a general pair of ints. A few notes:
 * <ul>
 * <li>It'd be great to have typed subclasses - 'ordered pair'...'range',
 * 'point'
 * <li>This should probably be a record (but that requires GWT update)
 * <li>This should *definitely* be immutable, as should all the other tuples
 * (StringPair et al)
 * </ul>
 * 
 * FIXME - alcina
 */
@JsonAutoDetect(
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE,
	fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Bean
@XmlAccessorType(XmlAccessType.FIELD)
@Registrations({ @Registration(JaxbContextRegistration.class), })
public final class IntPair implements Comparable<IntPair>, Serializable,
		Iterable<Integer>, TreeSerializable {
	/*
	 * later before earler; smaller before larger; (which handles overlaps) -
	 * this is a superset of contained is before containing
	 */
	public static class MostSpecificComparator implements Comparator<IntPair> {
		public static final transient MostSpecificComparator INSTANCE = new MostSpecificComparator();

		@Override
		public int compare(IntPair o1, IntPair o2) {
			{
				int cmp = o1.i1 - o2.i1;
				if (cmp != 0) {
					return cmp > 0 ? -1 : 1;
				}
			}
			{
				int cmp = o1.i2 - o2.i2;
				if (cmp != 0) {
					return cmp < 0 ? -1 : 1;
				}
			}
			return 0;
		}
	}

	public static class IntPairComparator implements Comparator<IntPair> {
		private boolean xAxis;

		public IntPairComparator(boolean xAxis) {
			this.xAxis = xAxis;
		}

		@Override
		public int compare(IntPair o1, IntPair o2) {
			if (xAxis) {
				return o1.i1 - o2.i1;
			} else {
				return o1.i2 - o2.i2;
			}
		}
	}

	public static enum IntPairRelation {
		NO_INTERSECTION, CONTAINS_ALL, CONTAINED_BY_ALL, CONTAINS_START,
		CONTAINS_END
	}

	/*
	 * If the ints are out-of-order, they will be ignored
	 */
	public static List<IntPair> asRangeList(Collection<Integer> ints) {
		int start = -1;
		List<IntPair> result = new ArrayList<>();
		for (Integer cursor : ints) {
			if (start == -1) {
				start = cursor;
			} else {
				if (start < cursor) {
					result.add(new IntPair(start, cursor));
					start = cursor;
				}
			}
		}
		return result;
	}

	public static boolean containedInRanges(List<IntPair> ranges,
			IntPair range) {
		return intersection(ranges, range) != null;
	}

	public static boolean containedInRanges(List<IntPair> ranges, int index) {
		return ranges.stream()
				.anyMatch(r -> r.containsExEnd(IntPair.point(index)));
	}

	public static boolean isContinuous(List<IntPair> pairs) {
		IntPair union = unionOf(pairs);
		return provideUncovered(pairs, union).isEmpty();
	}

	public static IntPair of(int i1, int i2) {
		return new IntPair(i1, i2);
	}

	public static IntPair parseIntPair(String string) {
		try {
			String cleaned = string.replaceAll("[\\[\\]]", "");
			String[] split = cleaned.split("[,-]");
			if (split.length == 2) {
				return new IntPair(Integer.parseInt(split[0]),
						Integer.parseInt(split[1]));
			}
			int point = Integer.parseInt(cleaned);
			return new IntPair(point, point);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	public static IntPair point(int i) {
		return new IntPair(i, i);
	}

	/**
	 * say, to provide a model of string regions not matched by
	 * regex.matcher().find()
	 */
	public static List<IntPair> provideUncovered(List<IntPair> covered,
			IntPair container) {
		List<IntPair> result = new ArrayList<IntPair>();
		for (int i = 0; i <= covered.size(); i++) {
			int from = i == 0 ? container.i1 : covered.get(i - 1).i2;
			int to = i == covered.size() ? container.i2 : covered.get(i).i1;
			if (from != to) {
				result.add(new IntPair(from, to));
			}
		}
		return result;
	}

	public static IntPair unionOf(List<IntPair> matchedRanges) {
		// i.e clone
		if (matchedRanges.isEmpty()) {
			return null;
		}
		IntPair result = matchedRanges.get(0).shiftRight(0);
		matchedRanges.forEach(ip -> {
			result.i1 = Math.min(ip.i1, result.i1);
			result.i2 = Math.max(ip.i2, result.i2);
		});
		return result;
	}

	public static IntPair intersection(List<IntPair> ranges, IntPair range) {
		for (IntPair intPair : ranges) {
			IntPair intersection = range.intersection(intPair);
			if (intersection != null && !intersection.isPoint()) {
				return intersection;
			}
		}
		return null;
	}

	public static List<IntPair> asContinuousRangeList(IntPair intPair,
			List<IntPair> truncateTos) {
		TreeSet<Integer> points = new TreeSet<>();
		Stream.concat(Stream.of(intPair), truncateTos.stream())
				.forEach(pair -> {
					points.add(pair.i1);
					points.add(pair.i2);
				});
		return asRangeList(points.stream().toList());
	}

	public static boolean sameStartAndAtLeastOnePoint(IntPair pair1,
			IntPair pair2) {
		return (pair1.i1 == pair2.i1) && (pair1.isPoint() || pair2.isPoint());
	}

	public static boolean sameEndAndAtLeastOnePoint(IntPair pair1,
			IntPair pair2) {
		return (pair1.i2 == pair2.i2) && (pair1.isPoint() || pair2.isPoint());
	}

	public static List<IntPair> mergeOverlapping(List<IntPair> pairs) {
		List<IntPair> result = new ArrayList<>();
		for (IntPair pair : pairs) {
			IntPair lastPair = Ax.last(result);
			if (lastPair != null && lastPair.intersectsWith(pair)) {
				lastPair.expandToInclude(pair);
			} else {
				result.add(pair);
			}
		}
		return result;
	}

	public int i1;

	public int i2;

	public IntPair() {
	}

	public IntPair(int i1, int i2) {
		super();
		this.i1 = i1;
		this.i2 = i2;
	}

	public void add(IntPair ip) {
		i1 += ip.i1;
		i2 += ip.i2;
	}

	public List<Integer> closedRange() {
		List<Integer> list = new ArrayList<>();
		for (int idx = i1; idx <= i2; idx++) {
			list.add(idx);
		}
		return list;
	}

	public IntPair closedToOpen() {
		return new IntPair(i1, i2 + 1);
	}

	public IntPairRelation compareBounds(IntPair o) {
		IntPair intersection = intersection(o);
		if (intersection == null) {
			return IntPairRelation.NO_INTERSECTION;
		}
		if (intersection.contains(this)) {
			return IntPairRelation.CONTAINED_BY_ALL;
		}
		if (intersection.contains(o)) {
			return IntPairRelation.CONTAINS_ALL;
		}
		if (intersection.contains(o.i1)) {
			return IntPairRelation.CONTAINS_START;
		} else {
			return IntPairRelation.CONTAINS_END;
		}
	}

	/*
	 * a range is before another if it starts before, or if starts are equal and
	 * it ends before the other
	 */
	@Override
	public int compareTo(IntPair ip) {
		return i1 < ip.i1 ? -1
				: i1 > ip.i1 ? 1 : i2 < ip.i2 ? -1 : i2 > ip.i2 ? 1 : 0;
	}

	/**
	 * Incluseive!!
	 * 
	 * @param i
	 * @return
	 */
	public boolean contains(int i) {
		return i >= i1 && i <= i2;
	}

	public boolean contains(IntPair other) {
		return other == null ? false : other.equals(intersection(other));
	}

	public boolean containsExAtLeastOneBoundary(IntPair other) {
		return contains(other) && (i1 < other.i1 || i2 > other.i2);
	}

	public boolean containsExBoundaries(IntPair other) {
		return contains(other) && i1 < other.i1 && i2 > other.i2;
	}

	public boolean containsExEnd(int i) {
		return i >= i1 && i < i2;
	}

	public boolean containsExEnd(IntPair other) {
		return contains(other) && i2 > other.i2;
	}

	public boolean continues(IntPair o) {
		return continues(o, 0);
	}

	public boolean continues(IntPair o, int tolerance) {
		return o.i2 <= i1 && i1 - tolerance <= o.i2;
	}

	public boolean continues(IntPair o, List<IntPair> fillers, int tolerance) {
		IntPair range = new IntPair(o.i1, o.i2);
		fillers.sort(null);
		for (IntPair filler : fillers) {
			if (filler.continues(range, tolerance)) {
				range.i2 = filler.i2;
			}
		}
		return continues(range, tolerance);
	}

	@Override
	public IntPair copy() {
		return new IntPair(i1, i2);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntPair) {
			IntPair ip = (IntPair) obj;
			return i1 == ip.i1 && i2 == ip.i2;
		}
		return false;
	}

	public void expand(int value) {
		i1 = i1 == 0 ? value : Math.min(i1, value);
		i2 = i2 == 0 ? value : Math.max(i2, value);
	}

	public void expandToInclude(IntPair mod) {
		IntPair union = union(mod);
		i1 = union.i1;
		i2 = union.i2;
	}

	public void expandToIncludeAllowNonContiguous(IntPair other) {
		i1 = Math.min(i1, other.i1);
		i2 = Math.max(i2, other.i2);
	}

	public int getI1() {
		return this.i1;
	}

	public int getI2() {
		return this.i2;
	}

	@Override
	public int hashCode() {
		return i1 << 16 ^ i2;
	}

	public IntPair intersection(IntPair other) {
		IntPair result = new IntPair(Math.max(i1, other.i1),
				Math.min(i2, other.i2));
		return result.i1 <= result.i2 ? result : null;
	}

	public IntPair intersectionOrZero(IntPair other) {
		IntPair result = intersection(other);
		return result == null ? new IntPair(0, 0) : result;
	}

	public boolean intersectsWith(IntPair other) {
		return intersection(other) != null;
	}

	public boolean intersectsWithNonPoint(IntPair other) {
		IntPair result = intersection(other);
		return result != null && !result.isPoint();
	}

	// as a possible vector in 1-space
	public boolean isPoint() {
		return i1 == i2;
	}

	public boolean isPositiveRange() {
		return i2 - i1 > 0;
	}

	public boolean isZero() {
		return i1 == 0 && i2 == 0;
	}

	@Override
	public Iterator<Integer> iterator() {
		return IntStream.rangeClosed(i1, i2).iterator();
	}

	public int length() {
		return i2 - i1;
	}

	public void max(IntPair ip) {
		i1 = i1 == 0 ? ip.i1 : Math.min(i1, ip.i1);
		i2 = i2 == 0 ? ip.i2 : Math.max(i1, ip.i2);
	}

	public int minDistance(IntPair other) {
		if (intersectsWith(other)) {
			return 0;
		}
		return Math.min(Math.abs(other.i1 - i2), Math.abs(other.i2 - i1));
	}

	public boolean overlapsWith(IntPair other) {
		IntPair intersection = intersection(other);
		return intersection != null && !Objects.equals(intersection, this)
				&& !Objects.equals(intersection, other);
	}

	public void setI1(int i1) {
		this.i1 = i1;
	}

	public void setI2(int i2) {
		this.i2 = i2;
	}

	public IntPair shiftRight(int offset) {
		return new IntPair(i1 + offset, i2 + offset);
	}

	public String simpleString() {
		return i1 + "," + i2;
	}

	public IntStream stream(boolean closed) {
		return closed ? IntStream.rangeClosed(i1, i2) : IntStream.range(i1, i2);
	}

	public String substring(String str) {
		return str.substring(Math.max(i1, 0), Math.min(i2, str.length()));
	}

	public void subtract(IntPair ip) {
		i1 -= ip.i1;
		i2 -= ip.i2;
	}

	public String toDashString() {
		return "[" + i1 + "-" + i2 + "]";
	}

	public String toDashStringOrPoint() {
		return isPoint() ? String.valueOf(i1) : i1 + "-" + i2;
	}

	public IntPair toEndPoint() {
		return new IntPair(i2, i2);
	}

	public IntPair toStartPoint() {
		return new IntPair(i1, i1);
	}

	@Override
	public String toString() {
		return "[" + i1 + "," + i2 + "]";
	}

	public int trimToRange(int i) {
		if (isPoint()) {
			return i1 - 1;
		}
		if (i < i1) {
			return i1;
		}
		if (i >= i2) {
			return i2 - 1;
		}
		return i;
	}

	public IntPair union(IntPair other) {
		if (!intersectsWith(other)) {
			return null;
		}
		return new IntPair(Math.min(i1, other.i1), Math.max(i2, other.i2));
	}

	public IntPair toLowestFirst() {
		if (i1 <= i2) {
			return this;
		} else {
			return of(i2, i1);
		}
	}

	public boolean isContiguousWith(IntPair o) {
		return i2 == o.i1;
	}
}
