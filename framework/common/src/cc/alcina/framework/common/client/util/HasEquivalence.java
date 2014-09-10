package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public interface HasEquivalence<T> {
	public boolean equivalentTo(T other);

	public static class HasEquivalenceHelper {
		/**
		 * Returns the objects from the first collection which have an equivalent in the second
		 */
		public static <T extends HasEquivalence> List<? super HasEquivalence> intersection(
				Collection<T> o1, Collection<T> o2) {
			List<? super HasEquivalence> result = new ArrayList<HasEquivalence>();
			for (Iterator<T> itr1 = o1.iterator(); itr1.hasNext();) {
				T t1 = itr1.next();
				for (Iterator<T> itr2 = o2.iterator(); itr2.hasNext();) {
					T t2 = itr2.next();
					if (t1.equivalentTo(t2)) {
						result.add(t1);
						break;
					}
				}
			}
			return result;
		}

		public static <T extends HasEquivalence> List<? super HasEquivalence> removeAll(
				Collection<T> o1, Collection<T> o2) {
			List<? super HasEquivalence> result = new ArrayList<HasEquivalence>();
			for (Iterator<T> itr1 = o1.iterator(); itr1.hasNext();) {
				T t1 = itr1.next();
				boolean add = true;
				for (Iterator<T> itr2 = o2.iterator(); itr2.hasNext();) {
					T t2 = itr2.next();
					if (t1.equivalentTo(t2)) {
						add = false;
					}
				}
				if (add) {
					result.add(t1);
				}
			}
			return result;
		}

		public static <T extends HasEquivalence> boolean equivalent(
				Collection<T> o1, Collection<T> o2) {
			return o1.size() == o2.size()
					&& intersection(o1, o2).size() == o1.size();
		}

		public static <T extends HasEquivalence> boolean contains(
				Collection<T> o1, T o2) {
			return !intersection(o1, Collections.singletonList(o2)).isEmpty();
		}
	}
}
