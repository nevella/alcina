package cc.alcina.framework.common.client.util;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup.MapAdapter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.domain.ArrayBackedIntegerMap;

/*
 * FIXME - dirndl 1x3 - cleanup? Also move refs to CollectionCreators.Bootstrap
 * to here
 *
 * This class optimises for both jdk[fastutil] vs js/gwt [hashmap] and for gwt
 * [uniquemap]
 */
public class AlcinaCollections {
	public static <V> Comparator<V> caseInsensitiveToStringOrder() {
		return new Comparator() {
			IdentityHashMap<Object, String> map = new IdentityHashMap<>();

			@Override
			public int compare(Object o1, Object o2) {
				String s1 = o1 == null ? null
						: map.computeIfAbsent(o1,
								o -> o.toString().toLowerCase());
				String s2 = o2 == null ? null
						: map.computeIfAbsent(o2,
								o -> o.toString().toLowerCase());
				if (s1 == null) {
					if (s2 == null) {
						return 0;
					} else {
						return -1;
					}
				}
				if (s2 == null) {
					return 1;
				}
				return s1.compareTo(s2);
			}
		};
	}

	public static <K, V> Map<K, V> newHashMap() {
		return CollectionCreators.Bootstrap.getHashMapCreator().create();
	}

	public static <T> Set<T> newHashSet() {
		return Registry.impl(CollectionCreators.HashSetCreator.class).create();
	}

	public static <K, V> Map<K, V> newLinkedHashMap() {
		return CollectionCreators.Bootstrap.getLinkedMapCreator().create();
	}

	/*
	 */
	public static <V> Map<Integer, V> newCoarseIntHashMap() {
		return extension.newCoarseIntHashMap();
	}

	public static <T> Set<T> newLinkedHashSet() {
		return new LinkedHashSet<>();
		// return
		// CollectionCreators.Bootstrap.getLinkedHashSetCreator().create();
	}

	public static <V> IntLookup<V> newIntLookup() {
		return extension.newIntLookup();
	}

	public interface Extension {
		<T> Set<T> newUniqueSet();

		<K, V> Map<K, V> newUniqueMap();

		<V> Map<Integer, V> newCoarseIntHashMap();

		<V> IntLookup<V> newIntLookup();

		public static class Default implements Extension {
			@Override
			public <T> Set<T> newUniqueSet() {
				return newHashSet();
			}

			public <K, V> Map<K, V> newUniqueMap() {
				return newHashMap();
			}

			@Override
			public <V> Map<Integer, V> newCoarseIntHashMap() {
				return new ArrayBackedIntegerMap<>(1000);
			}

			@Override
			public <V> IntLookup<V> newIntLookup() {
				return new MapAdapter<V>();
			}
		}
	}

	public static Extension extension;

	public static <T> Set<T> newUniqueSet() {
		return extension.newUniqueSet();
	}

	// can be implemented by a js map js.client side (object identity keys, not
	// equals() based) - note that a hashmap will still work, although it should
	// be an identityhashmap (non js.client)
	public static <K, V> Map<K, V> newUniqueMap() {
		return extension.newUniqueMap();
	}

	// currently no GWT implementation - but will use es6 WeakMap
	public static <K, V> Map<K, V> newWeakMap() {
		return Registry.impl(CollectionCreators.WeakMapCreator.class).create();
	}

	public static <T> Set<T> newHashSet(int size) {
		return Registry.impl(CollectionCreators.HashSetCreator.class)
				.create(size);
	}
}
