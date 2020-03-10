package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.FromObjectKeyValueMapper;
import cc.alcina.framework.common.client.util.CommonUtils.ThreeWaySetResult;

public interface HasEquivalence<T> {
	public boolean equivalentTo(T other);

	public abstract static class HasEquivalenceAdapter<T, E extends HasEquivalenceAdapter>
			implements HasEquivalenceHash<E> {
		public T o;

		public T left;

		public T right;

		public HasEquivalenceAdapter(T referent) {
			this.o = referent;
		}

		public T getReferent() {
			return o;
		}

		@Override
		public String toString() {
			return o.toString();
		}
	}

	public static class HasEquivalenceAdapterIdentity extends
			HasEquivalenceAdapter<HasEquivalence, HasEquivalenceAdapterIdentity> {
		public HasEquivalenceAdapterIdentity(HasEquivalence referent) {
			super(referent);
		}

		@Override
		public int equivalenceHash() {
			return 0;
		}

		@Override
		public boolean equivalentTo(HasEquivalenceAdapterIdentity other) {
			HasEquivalence i = other.o;
			return o.equivalentTo(i);
		}
	}

	public static class HasEquivalenceAdapterObjectIdentity<T> extends
			HasEquivalenceAdapter<T, HasEquivalenceAdapterObjectIdentity> {
		public HasEquivalenceAdapterObjectIdentity(T referent) {
			super(referent);
		}

		@Override
		public int equivalenceHash() {
			return System.identityHashCode(o);
		}

		@Override
		public boolean equivalentTo(HasEquivalenceAdapterObjectIdentity other) {
			return o == other.o;
		}
	}

	public static interface HasEquivalenceHash<T> extends HasEquivalence<T> {
		public int equivalenceHash();
	}

	public static class HasEquivalenceHelper {
		public static <T, V extends HasEquivalenceAdapter<T, ?>> boolean
				allEquivalent(Collection<T> o1, Function<T, V> mapper) {
			if (o1.isEmpty()) {
				return true;
			}
			List<V> l1 = o1.stream().map(mapper).collect(Collectors.toList());
			List<V> l2 = Collections.singletonList(o1.iterator().next())
					.stream().map(mapper).collect(Collectors.toList());
			List<V> intersection = (List) HasEquivalenceHelper.intersection(l1,
					l2);
			return intersection.size() == l1.size();
		}

		public static boolean argwiseEquivalent(Object... args) {
			if (args.length % 2 != 0) {
				throw new RuntimeException(
						"Array length must be divisible by two");
			}
			for (int i = 0; i < args.length; i += 2) {
				Object o1 = args[i];
				Object o2 = args[i + 1];
				if (o1 instanceof HasEquivalence
						&& o2 instanceof HasEquivalence) {
					if (!((HasEquivalence) o1)
							.equivalentTo((HasEquivalence) o2)) {
						return false;
					}
				} else {
					if (!Objects.equals(o1, o2)) {
						return false;
					}
				}
			}
			return true;
		}

		public static <T extends HasEquivalence> boolean
				contains(Collection<T> o1, T o2) {
			return !intersection(o1, Collections.singletonList(o2)).isEmpty();
		}

		public static <T extends HasEquivalence> void
				deDuplicate(Collection<T> list) {
			List<T> duplicates = listDuplicates(list);
			list.removeAll(duplicates);
		}

		public static <T, V extends HasEquivalenceAdapter<T, ?>> List<T>
				deDuplicate(Collection<T> o1, Function<T, V> mapper) {
			List<V> l1 = o1.stream().map(mapper).collect(Collectors.toList());
			List<V> duplicates = listDuplicates(l1);
			l1.removeAll(duplicates);
			return l1.stream().map(l -> l.o).collect(Collectors.toList());
		}

		public static <C extends HasEquivalence> Predicate<C>
				deDuplicateFilter() {
			return new DeduplicateHasEquivalencePredicate<>();
		}

		public static <T extends HasEquivalence> boolean
				equivalent(Collection<T> o1, Collection<T> o2) {
			if (o1 == null || o2 == null) {
				return o1 == o2;
			}
			return o1.size() == o2.size()
					&& intersection(o1, o2).size() == o1.size();
		}

		public static <T extends HasEquivalence> T
				getEquivalent(Collection<T> o1, T o2) {
			return (T) CommonUtils
					.first(intersection(o1, Collections.singletonList(o2)));
		}

		public static <T, V extends HasEquivalenceAdapter<T, ?>> T
				getEquivalent(Collection<T> o1, T o2, Function<T, V> mapper) {
			List<V> l1 = o1.stream().map(mapper).collect(Collectors.toList());
			List<V> l2 = Collections.singletonList(o2).stream().map(mapper)
					.collect(Collectors.toList());
			List<V> intersection = (List) HasEquivalenceHelper.intersection(l1,
					l2);
			return intersection.isEmpty() ? null
					: CommonUtils.first(intersection).o;
		}

		public static <T extends HasEquivalenceHash>
				List<HasEquivalenceTuple<T>>
				getEquivalents(Collection<T> left, Collection<T> right) {
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

		public static <T extends HasEquivalence> HasEquivalenceHashMap<T>
				getHashed(Collection<T> coll) {
			if (coll == null || coll.isEmpty() || (!(coll.iterator()
					.next() instanceof HasEquivalenceHash))) {
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

		/**
		 * Returns the objects from the first collection which have an
		 * equivalent in the second
		 */
		public static <T extends HasEquivalence> List<? super HasEquivalence>
				intersection(Collection<T> o1, Collection<T> o2) {
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

		public static <T extends HasEquivalence> List<T>
				listDuplicates(Collection<T> o1) {
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

		public static <T extends HasEquivalenceHash> List<T>
				listDuplicatesHashed(Collection<T> o1) {
			HasEquivalenceHashMap<T> passed = new HasEquivalenceHashMap<T>();
			List<T> duplicates = new ArrayList<T>();
			for (T t : o1) {
				boolean duplicate = false;
				for (T pass : passed.getAndEnsure(t.equivalenceHash())) {
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

		public static <T extends HasEquivalence> Multimap<T, List<T>>
				mapDuplicates(Collection<T> o1) {
			List<T> passed = new ArrayList<T>();
			List<T> duplicates = new ArrayList<T>();
			Multimap<T, List<T>> result = new Multimap<>();
			for (T t : o1) {
				boolean duplicate = false;
				for (T pass : passed) {
					if (pass.equivalentTo(t)) {
						duplicates.add(t);
						result.add(pass, t);
						duplicate = true;
						break;
					}
				}
				if (!duplicate) {
					passed.add(t);
					result.add(t, t);
				}
			}
			return result;
		}

		public static <T extends HasEquivalence> Collection<T>
				maybeHashedCorrespondents(T sameHashAs,
						Collection<T> collection,
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

		public static <T extends HasEquivalence> List<? super HasEquivalence>
				removeAll(Collection<T> removeFrom,
						Collection<T> equivalentsToRemove) {
			List<? super HasEquivalence> result = new ArrayList<HasEquivalence>();
			HasEquivalenceHashMap<T> hashed = getHashed(equivalentsToRemove);
			for (Iterator<T> itr1 = removeFrom.iterator(); itr1.hasNext();) {
				T t1 = itr1.next();
				boolean add = true;
				for (Iterator<T> itr2 = maybeHashedCorrespondents(t1,
						equivalentsToRemove, hashed).iterator(); itr2
								.hasNext();) {
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

		public static <T extends HasEquivalenceHash> ThreeWaySetResult<T>
				threeWaySplit(Collection<T> c1, Collection<T> c2) {
			ThreeWaySetResult<T> result = new ThreeWaySetResult<>();
			Set intersection = new LinkedHashSet<>((List) intersection(c1, c2));
			result.intersection = intersection;
			result.firstOnly = new LinkedHashSet<>(
					(List) removeAll(c1, intersection));
			result.secondOnly = new LinkedHashSet<>(
					(List) removeAll(c2, intersection));
			return result;
		}

		public static <T, V extends HasEquivalenceAdapter<T, ?>>
				ThreeWaySetResult<V> threeWaySplit(Collection<T> c1,
						Collection<T> c2, Function<T, V> mapper,
						BiFunction<T, Collection<T>, T> correspondenceMapper) {
			List<V> l1 = c1.stream().map(mapper).collect(Collectors.toList());
			List<V> l2 = c2.stream().map(mapper).collect(Collectors.toList());
			ThreeWaySetResult<V> result = threeWaySplit(l1, l2);
			result.firstOnly
					.forEach(v -> v.left = correspondenceMapper.apply(v.o, c1));
			result.intersection
					.forEach(v -> v.left = correspondenceMapper.apply(v.o, c1));
			result.intersection.forEach(
					v -> v.right = correspondenceMapper.apply(v.o, c2));
			result.secondOnly.forEach(
					v -> v.right = correspondenceMapper.apply(v.o, c2));
			return result;
		}

		public static class DeduplicateHasEquivalencePredicate<C extends HasEquivalence>
				implements Predicate<C> {
			List<C> seen = new ArrayList<>();

			@Override
			public boolean test(C t) {
				if (seen.stream().anyMatch(c -> c.equivalentTo(t))) {
					return false;
				} else {
					seen.add(t);
					return true;
				}
			}
		}
	}

	public static class HasEquivalenceTuple<T> {
		public T left;

		public T right;

		public HasEquivalenceTuple(T left, T right) {
			this.left = left;
			this.right = right;
		}
	}
}
