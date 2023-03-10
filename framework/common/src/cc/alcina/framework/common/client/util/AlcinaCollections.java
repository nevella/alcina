package cc.alcina.framework.common.client.util;

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
	public static <K, V> Map<K, V> newHashMap() {
		return CollectionCreators.Bootstrap.getHashMapCreator().create();
	}

	public static <T> Set<T> newHashSet() {
		return Registry.impl(CollectionCreators.HashSetCreator.class).create();
	}

	public static <K, V> Map<K, V> newLinkedHashMap() {
		return CollectionCreators.Bootstrap.getHashMapCreator().create();
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
