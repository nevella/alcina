package cc.alcina.framework.common.client.domain;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Reflected
@Registration.Singleton
public class DomainCollections {
	public static DomainCollections get() {
		return Registry.impl(DomainCollections.class);
	}

	public Set createLightSet() {
		return new LightSet<>();
	}

	public Set createSortedSet() {
		return new TreeSet<>();
	}

	public <K, V> Map<K, V> createUnsortedMap() {
		return new LinkedHashMap<>();
	}

	public <E> Set<E> createUnsortedSet() {
		return new LinkedHashSet<>();
	}
}
