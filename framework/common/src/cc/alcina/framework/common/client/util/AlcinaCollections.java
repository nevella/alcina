package cc.alcina.framework.common.client.util;

import java.util.Map;
import java.util.Set;

import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * FIXME - dirndl 1x1d - uniquemap anywhere? this is both optimise for
 * jdk[fastutil] vs js/gwt [hashmap] and optimise for gwt [uniquemap]
 */
public class AlcinaCollections {
	public static <K, V> Map<K, V> newHashMap() {
		return CollectionCreators.Bootstrap.getHashMapCreator().create();
	}

	public static <T> Set<T> newHashSet() {
		return Registry.impl(CollectionCreators.HashSetCreator.class).create();
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
}
