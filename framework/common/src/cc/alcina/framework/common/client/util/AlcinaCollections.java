package cc.alcina.framework.common.client.util;

import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class AlcinaCollections {
	public static <T> Set<T> newHashSet() {
		return Registry.impl(CollectionCreators.HashSetCreator.class).create();
	}

	public <K, V> Map<K, V> newHashMap() {
		return CollectionCreators.Bootstrap.getHashMapCreator().create();
	}
}
