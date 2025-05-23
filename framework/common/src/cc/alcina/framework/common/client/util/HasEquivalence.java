package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.reflection.Reflections;

/*
 * 'equivalence' is a relation between instances of a class similar to equals -
 * but weaker (multiple objects can be equivalent, by design)
 */
public interface HasEquivalence<T> {
	public static boolean areEquivalent(HasEquivalence o1, HasEquivalence o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o1.equivalentTo(o2);
		}
	}

	public static <T, E extends HasEquivalenceAdapter> boolean areEquivalent(
			Class<? extends HasEquivalenceAdapter<T, E>> adapterClass, T o1,
			T o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			HasEquivalenceAdapter<T, E> a1 = Reflections
					.newInstance(adapterClass).withReferent(o1);
			HasEquivalenceAdapter<T, E> a2 = Reflections
					.newInstance(adapterClass).withReferent(o2);
			return a1.equivalentTo((E) a2);
		}
	}

	public int equivalenceHash();

	public boolean equivalentTo(T other);

	public abstract static class HasEquivalenceAdapter<T, E extends HasEquivalenceAdapter>
			implements HasEquivalence<E> {
		public T o;

		public T left;

		public T right;

		public <HEA extends HasEquivalenceAdapter<T, E>> HEA withReferent(T o) {
			this.o = o;
			return (HEA) this;
		}

		public HasEquivalenceAdapter() {
		}

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

	public abstract static class HasEquivalenceAdapterString<T, E extends HasEquivalenceAdapterString>
			extends HasEquivalenceAdapter<T, E>
			implements HasEquivalenceString<E> {
		public HasEquivalenceAdapterString(T referent) {
			super(referent);
		}
	}

	public static class HasEquivalenceHelper {
		public static final String CONTEXT_IGNORE_FOR_DEBUGGING = HasEquivalenceHelper.class
				.getName() + ".CONTEXT_IGNORE_FOR_DEBUGGING";

		public static boolean debugInequivalence;

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

		private static <T extends HasEquivalence> void
				checkUnique(List<T> list) {
			if (list.size() < 1) {
				return;
			}
			HasEquivalenceMap<T> lookup = new HasEquivalenceMap<>(list);
			for (T element : list) {
				List<T> equivalents = lookup.getEquivalents(element);
				if (equivalents.size() > 1) {
					throw new NotUniqueException(equivalents);
				}
			}
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
			if (o1.size() == o2.size()) {
				if (debugInequivalence) {
					try {
						LooseContext.pushWithTrue(CONTEXT_IGNORE_FOR_DEBUGGING);
						return intersection(o1, o2).size() == o1.size();
					} finally {
						LooseContext.pop();
					}
				} else {
					return intersection(o1, o2).size() == o1.size();
				}
			} else {
				return false;
			}
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

		public static <T extends HasEquivalence> List<HasEquivalenceTuple<T>>
				getEquivalents(Collection<T> left, Collection<T> right) {
			return getEquivalents(left, right, false, true);
		}

		public static <T extends HasEquivalence> List<HasEquivalenceTuple<T>>
				getEquivalents(Collection<T> left, Collection<T> right,
						boolean alsoUnmatched, boolean requireUnique) {
			List<HasEquivalenceTuple<T>> result = new ArrayList<HasEquivalence.HasEquivalenceTuple<T>>();
			HasEquivalenceMap<T> leftHashedMap = getHashed(left);
			HasEquivalenceMap<T> rightHashedMap = getHashed(right);
			HasEquivalenceMap<T> leftMatched = new HasEquivalenceMap<>();
			HasEquivalenceMap<T> rightMatched = new HasEquivalenceMap<>();
			if (leftHashedMap == null || rightHashedMap == null) {
			} else {
				for (Entry<Integer, List<T>> entry : leftHashedMap.entrySet()) {
					List<T> leftList = entry.getValue();
					List<T> rightList = rightHashedMap
							.getAndEnsure(entry.getKey());
					if (requireUnique) {
						checkUnique(leftList);
						checkUnique(rightList);
					}
					for (T leftItem : leftList) {
						for (T rightItem : rightList) {
							if (leftItem.equivalentTo(rightItem)) {
								result.add(new HasEquivalenceTuple<T>(leftItem,
										rightItem));
								leftMatched.addUnique(leftItem);
								rightMatched.addUnique(rightItem);
							}
						}
					}
				}
			}
			if (alsoUnmatched) {
				left.stream().filter(t -> !leftMatched.containsHehValue(t))
						.forEach(t -> result
								.add(new HasEquivalenceTuple<T>(t, null)));
				right.stream().filter(t -> !rightMatched.containsHehValue(t))
						.forEach(t -> result
								.add(new HasEquivalenceTuple<T>(null, t)));
			}
			return result;
		}

		public static <T extends HasEquivalence> HasEquivalenceMap<T>
				getHashed(Collection<T> coll) {
			if (coll == null || coll.isEmpty()
					|| (!(coll.iterator().next() instanceof HasEquivalence))) {
				return null;
			}
			HasEquivalenceMap<T> result = new HasEquivalenceMap<T>();
			result.putAll(coll.stream().collect(AlcinaCollectors.toKeyMultimap(
					o -> ((HasEquivalence<T>) o).equivalenceHash())));
			return result;
		}

		public static int hash(Object... args) {
			int hash = 0;
			for (int i = 0; i < args.length; i++) {
				Object o = args[i];
				if (o instanceof HasEquivalence) {
					hash ^= ((HasEquivalence) o).equivalenceHash();
				} else if (o instanceof Collection) {
					for (Object o2 : (Collection) o) {
						hash ^= hash(o2);
					}
				} else {
					hash ^= Objects.hash(o);
				}
			}
			return hash;
		}

		/**
		 * Returns the objects from the first collection which have an
		 * equivalent in the second
		 */
		public static <T extends HasEquivalence> List<? super HasEquivalence>
				intersection(Collection<T> o1, Collection<T> o2) {
			List<? super HasEquivalence> result = new ArrayList<>();
			HasEquivalenceMap<T> hashed = getHashed(o2);
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

		public static <T extends HasEquivalence> List<T>
				listDuplicatesHashed(Collection<T> o1) {
			HasEquivalenceMap<T> passed = new HasEquivalenceMap<T>();
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
						Collection<T> collection, HasEquivalenceMap<T> hashed) {
			if (hashed == null) {
				return collection;
			}
			int equivalenceHash = sameHashAs.equivalenceHash();
			if (hashed.containsKey(equivalenceHash)) {
				return hashed.get(equivalenceHash);
			} else {
				return Collections.EMPTY_LIST;
			}
		}

		public static <T extends Entity & HasEquivalence> void mergeTransforms(
				Collection<T> existing, Collection<T> generated) {
			// compare in order (generated,existing) so the intersection will be
			// the generated objects (for which we remove transforms)
			Intersection<T> split = threeWaySplit(generated, existing);
			split.secondOnly.forEach(Entity::delete);
			TransformManager.get()
					.removeTransformsForObjects(split.intersection);
		}

		public static <T extends HasEquivalence> List<? super HasEquivalence>
				removeAll(Collection<T> removeFrom,
						Collection<T> equivalentsToRemove) {
			List<? super HasEquivalence> result = new ArrayList<>();
			HasEquivalenceMap<T> hashed = getHashed(equivalentsToRemove);
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

		public static <T extends HasEquivalence> Intersection<T>
				threeWaySplit(Collection<T> c1, Collection<T> c2) {
			Intersection<T> result = new Intersection<>();
			Set intersection = new LinkedHashSet<>((List) intersection(c1, c2));
			result.intersection = intersection;
			result.firstOnly = new LinkedHashSet<>(
					(List) removeAll(c1, intersection));
			result.secondOnly = new LinkedHashSet<>(
					(List) removeAll(c2, intersection));
			return result;
		}

		public static <T, V extends HasEquivalenceAdapter<T, ?>> Intersection<V>
				threeWaySplit(Collection<T> c1, Collection<T> c2,
						Function<T, V> mapper,
						BiFunction<T, Collection<T>, T> correspondenceMapper) {
			List<V> l1 = c1.stream().map(mapper).collect(Collectors.toList());
			List<V> l2 = c2.stream().map(mapper).collect(Collectors.toList());
			Intersection<V> result = threeWaySplit(l1, l2);
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

		public static class NotUniqueException extends RuntimeException {
			private List equivalents;

			public NotUniqueException(List equivalents) {
				this.equivalents = equivalents;
			}

			@Override
			public String toString() {
				return Ax.format("Not unique: %s", equivalents);
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

		@Override
		public String toString() {
			return Ax.format("[%s :: %s]", left, right);
		}
	}

	public static class PairwiseEquivalenceString<T extends HasEquivalenceString>
			implements HasEquivalenceString<T> {
		public static <T extends HasEquivalenceString>
				Intersection<PairwiseEquivalenceString<T>>
				split(Collection<T> left, Collection<T> right) {
			Map<String, T> leftMap = left.stream().collect(AlcinaCollectors
					.toKeyMap(HasEquivalenceString::equivalenceString));
			Map<String, T> rightMap = right.stream().collect(AlcinaCollectors
					.toKeyMap(HasEquivalenceString::equivalenceString));
			Preconditions.checkArgument(
					leftMap.size() == left.size()
							&& rightMap.size() == right.size(),
					"Non-unique keys");
			Intersection<PairwiseEquivalenceString<T>> result = new Intersection<>();
			Intersection<String> keySplit = Intersection.of(leftMap.keySet(),
					rightMap.keySet());
			result.firstOnly = keySplit.firstOnly.stream()
					.map(k -> new PairwiseEquivalenceString<>(k, leftMap.get(k),
							rightMap.get(k)))
					.collect(AlcinaCollectors.toLinkedHashSet());
			result.intersection = keySplit.intersection.stream()
					.map(k -> new PairwiseEquivalenceString<>(k, leftMap.get(k),
							rightMap.get(k)))
					.collect(AlcinaCollectors.toLinkedHashSet());
			result.secondOnly = keySplit.secondOnly.stream()
					.map(k -> new PairwiseEquivalenceString<>(k, leftMap.get(k),
							rightMap.get(k)))
					.collect(AlcinaCollectors.toLinkedHashSet());
			return result;
		}

		public String key;

		public T left;

		public T right;

		private PairwiseEquivalenceString(String key, T left, T right) {
			this.key = key;
			this.left = left;
			this.right = right;
		}

		@Override
		public String equivalenceString() {
			return key;
		}
	}

	public static <T extends HasEquivalenceString> Predicate<? super T>
			unique() {
		return new UniquePredicate<>();
	}

	static class UniquePredicate<T extends HasEquivalenceString>
			implements Predicate<T> {
		Set<String> equivalenceStrings = AlcinaCollections.newHashSet();

		@Override
		public boolean test(T t) {
			return equivalenceStrings.add(t.equivalenceString());
		}
	}
}
