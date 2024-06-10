package cc.alcina.framework.common.client.util;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

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

	public static <T> Set<T> newLinkedHashSet() {
		return new LinkedHashSet<>();
	}

	public static <T> Set<T> newUniqueSet() {
		return GWT.isScript() ? JsUniqueSet.create() : newHashSet();
	}

	// can be implemented by a js map js.client side (object identity keys, not
	// equals() based) - note that a hashmap will still work, although it should
	// be an identityhashmap (non js.client)
	public static <K, V> Map<K, V> newUnqiueMap() {
		return GWT.isScript() ? JsUniqueMap.create() : newHashMap();
	}

	// currently no GWT implementation - but will use es6 WeakMap
	public static <K, V> Map<K, V> newWeakMap() {
		return Registry.impl(CollectionCreators.WeakMapCreator.class).create();
	}
}
