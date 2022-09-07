package cc.alcina.framework.common.client.util;

import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class AlcinaCollections {
	public static <K, V> Map<K, V> newHashMap() {
		return CollectionCreators.Bootstrap.getHashMapCreator().create();
	}

	public static <T> Set<T> newHashSet() {
		return Registry.impl(CollectionCreators.HashSetCreator.class).create();
	}
}
