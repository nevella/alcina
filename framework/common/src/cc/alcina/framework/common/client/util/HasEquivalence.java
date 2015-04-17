package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.FromObjectKeyValueMapper;
import cc.alcina.framework.common.client.util.CommonUtils.ThreeWaySetResult;

public interface HasEquivalence<T> {
	public boolean equivalentTo(T other);

	public static interface HasEquivalenceHash<T> extends HasEquivalence<T> {
		public int equivalenceHash();
	}

	public static class HasEquivalenceTuple<T> {
		public T left;

		public T right;

		public HasEquivalenceTuple(T left, T right) {
			this.left = left;
			this.right = right;
		}
	}

	public static class HasEquivalenceHelper {
		public static <T extends HasEquivalence> boolean contains(
				Collection<T> o1, T o2) {
			return !intersection(o1, Collections.singletonList(o2)).isEmpty();
		}

		public static <T extends HasEquivalence> boolean equivalent(
				Collection<T> o1, Collection<T> o2) {
			return o1.size() == o2.size()
					&& intersection(o1, o2).size() == o1.size();
		}

		public static <T extends HasEquivalence> T getEquivalent(
				Collection<T> o1, T o2) {
			return (T) CommonUtils.first(intersection(o1,
					Collections.singletonList(o2)));
		}

		public static <T extends HasEquivalenceHash> ThreeWaySetResult<T> threeWaySplit(
				Collection<T> c1, Collection<T> c2) {
			ThreeWaySetResult<T> result = new ThreeWaySetResult<T>();
			Set intersection = new LinkedHashSet<T>((List) intersection(c1, c2));
			result.intersection = intersection;
			result.firstOnly = new LinkedHashSet<T>(removeAll(c1, intersection));
			result.secondOnly = new LinkedHashSet<T>(
					removeAll(c2, intersection));
			return result;
		}

		public static <T extends HasEquivalence> HasEquivalenceHashMap<T> getHashed(
				Collection<T> coll) {
			if (coll == null
					|| coll.isEmpty()
					|| (!(coll.iterator().next() instanceof HasEquivalenceHash))) {
				return null;
			}
			HasEquivalenceHashMap<T> result = new HasEquivalenceHashMap<T>();
			result.putAll(CollectionFilters.multimap(coll,
					new FromObjectKeyValueMapper<Integer, T>() {
						@Override
						public Integer getKey(T o) {
							return ((HasEquivalenceHash<T>) o)
									.equivalenceHash();
						}
					}));
			return result;
		}

		public static <T extends HasEquivalenceHash> List<HasEquivalenceTuple<T>> getEquivalents(
				Collection<T> left, Collection<T> right) {
			List<HasEquivalenceTuple<T>> result = new ArrayList<HasEquivalence.HasEquivalenceTuple<T>>();
			HasEquivalenceHashMap<T> lMap = getHashed(left);
			HasEquivalenceHashMap<T> rMap = getHashed(right);
			if (lMap == null || rMap == null) {
				return result;
			}
			for (Entry<Integer, List<T>> entry : lMap.entrySet()) {
				List<T> leftList = entry.getValue();
				List<T> rightList = rMap.getAndEnsure(entry.getKey());
				for (T leftItem : leftList) {
					for (T rightItem : rightList) {
						if (leftItem.equivalentTo(rightItem)) {
							result.add(new HasEquivalenceTuple<T>(leftItem,
									rightItem));
						}
					}
				}
			}
			return result;
		}

		/**
		 * Returns the objects from the first collection which have an
		 * equivalent in the second
		 */
		public static <T extends HasEquivalence> List<? super HasEquivalence> intersection(
				Collection<T> o1, Collection<T> o2) {
			List<? super HasEquivalence> result = new ArrayList<HasEquivalence>();
			HasEquivalenceHashMap<T> hashed = getHashed(o2);
			for (Iterator<T> itr1 = o1.iterator(); itr1.hasNext();) {
				T t1 = itr1.next();
				for (Iterator<T> itr2 = maybeHashedCorrespondents(t1, o2,
						hashed).iterator(); itr2.hasNext();) {
					T t2 = itr2.next();
					if (t1.equivalentTo(t2)) {
						result.add(t1);
						break;
					}
				}
			}
			return result;
		}

		public static <T extends HasEquivalence> List<T> listDuplicates(
				Collection<T> o1) {
			List<T> passed = new ArrayList<T>();
			List<T> duplicates = new ArrayList<T>();
			for (T t : o1) {
				boolean duplicate = false;
				for (T pass : passed) {
					if (pass.equivalentTo(t)) {
						duplicates.add(t);
						duplicate = true;
						break;
					}
				}
				if (!duplicate) {
					passed.add(t);
				}
			}
			return duplicates;
		}

		public static <T extends HasEquivalence> Collection<T> maybeHashedCorrespondents(
				T sameHashAs, Collection<T> collection,
				HasEquivalenceHashMap<T> hashed) {
			if (hashed == null) {
				return collection;
			}
			int equivalenceHash = ((HasEquivalenceHash<T>) sameHashAs)
					.equivalenceHash();
			if (hashed.containsKey(equivalenceHash)) {
				return hashed.get(equivalenceHash);
			} else {
				return Collections.EMPTY_LIST;
			}
		}

		public static <T extends HasEquivalence> List<? super HasEquivalence> removeAll(
				Collection<T> o1, Collection<T> o2) {
			List<? super HasEquivalence> result = new ArrayList<HasEquivalence>();
			HasEquivalenceHashMap<T> hashed = getHashed(o2);
			for (Iterator<T> itr1 = o1.iterator(); itr1.hasNext();) {
				T t1 = itr1.next();
				boolean add = true;
				for (Iterator<T> itr2 = maybeHashedCorrespondents(t1, o2,
						hashed).iterator(); itr2.hasNext();) {
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
	}
}
