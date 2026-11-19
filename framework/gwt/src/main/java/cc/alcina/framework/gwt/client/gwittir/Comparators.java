package cc.alcina.framework.gwt.client.gwittir;

import java.util.Comparator;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CachingToStringComparator;

public class Comparators {
	public static class ClassEquivalenceComparator implements Comparator {
		static final CachingToStringComparator INSTANCE = new CachingToStringComparator();

		public int compare(Object o1, Object o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			if (o2 == null) {
				return 1;
			}
			return o1.getClass() == o2.getClass() ? 0
					: INSTANCE.compare(o1, o2);
		}
	}

	/** note - not transitive...uh, it's a gwittir thing **/
	public static class EqualsComparator implements Comparator {
		public static final EqualsComparator INSTANCE = new EqualsComparator();

		public int compare(Object o1, Object o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			return o1.equals(o2) ? 0 : -1;
		}
	}

	@Reflected
	public static class IdComparator implements Comparator<HasId> {
		public static final Comparators.EqualsComparator INSTANCE = new Comparators.EqualsComparator();

		public int compare(HasId o1, HasId o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			if (o2 == null) {
				return 1;
			}
			return Long.valueOf(o1.getId()).compareTo(o2.getId());
		}
	}
}
