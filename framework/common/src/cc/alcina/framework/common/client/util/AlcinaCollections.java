package cc.alcina.framework.common.client.util;

import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class AlcinaCollections {
	public static <T> Set<T> newHashMap() {
		return Registry.impl(CollectionCreators.HashSetCreator.class).create();
	}
}
