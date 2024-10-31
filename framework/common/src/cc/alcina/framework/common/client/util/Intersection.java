package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public class Intersection<T> {
	public Set<T> firstOnly;

	public Set<T> secondOnly;

	public Set<T> intersection;

	public Stream<T> delta() {
		return Stream.concat(firstOnly.stream(), secondOnly.stream());
	}

	public boolean isEmpty() {
		return firstOnly.isEmpty() && secondOnly.isEmpty()
				&& intersection.isEmpty();
	}

	public boolean isIntersectionOnly() {
		return firstOnly.isEmpty() && secondOnly.isEmpty();
	}

	public boolean hasIntersection() {
		return intersection.size() > 0;
	}

	public String toSizes() {
		return CommonUtils.format("First: %s\tBoth: %s\tSecond: %s",
				firstOnly.size(), intersection.size(), secondOnly.size());
	}

	public String toSizes(String firstType, String secondType) {
		return CommonUtils.format("%s: %s\tBoth: %s\t%s: %s", firstType,
				firstOnly.size(), intersection.size(), secondType,
				secondOnly.size());
	}

	@Override
	public String toString() {
		return CommonUtils.format("First: %s\nBoth: %s\nSecond: %s", firstOnly,
				intersection, secondOnly);
	}

	/**
	 * Create an {@link Intersction}, using identity rather than equals to
	 * determine the intersection
	 * 
	 * @param <T>
	 *            the type of the Intersection elements
	 * @param c1
	 *            the first collection
	 * @param c2
	 *            the second collection
	 * @return an Intersection structure
	 * 
	 */
	public static <T> Intersection<T> ofIdentity(Collection<T> c1,
			Collection<T> c2) {
		Intersection<T> result = new Intersection<T>();
		IdentityHashMap m1 = new IdentityHashMap();
		IdentityHashMap m2 = new IdentityHashMap();
		c1.forEach(o -> m1.put(o, true));
		c2.forEach(o -> m2.put(o, true));
		result.firstOnly = m1.keySet();
		result.secondOnly = m2.keySet();
		IdentityHashMap intersection = new IdentityHashMap();
		result.intersection = intersection.keySet();
		for (Object o : m1.keySet()) {
			if (m2.keySet().contains(o)) {
				intersection.put(o, true);
			}
		}
		result.firstOnly.removeAll(result.intersection);
		result.secondOnly.removeAll(result.intersection);
		return result;
	}

	/**
	 * Create an {@link Intersction}, using Object.equals to determine the
	 * intersection
	 * 
	 * @param <T>
	 *            the type of the Intersection elements
	 * @param c1
	 *            the first collection
	 * @param c2
	 *            the second collection
	 * @return an Intersection structure
	 * 
	 */
	public static <T> Intersection<T> of(Collection<T> c1, Collection<T> c2) {
		Intersection<T> result = new Intersection<T>();
		Set intersection = CommonUtils.intersection(c1, c2);
		result.intersection = intersection;
		result.firstOnly = new LinkedHashSet<T>(c1);
		result.secondOnly = new LinkedHashSet<T>(c2);
		result.firstOnly.removeAll(intersection);
		result.secondOnly.removeAll(intersection);
		return result;
	}
}