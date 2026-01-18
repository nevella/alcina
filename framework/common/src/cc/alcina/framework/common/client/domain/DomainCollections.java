package cc.alcina.framework.common.client.domain;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;

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
		return AlcinaCollections.newLinkedHashMap();
	}

	public <E> Set<E> createUnsortedSet() {
		return AlcinaCollections.newLinkedHashSet();
	}
}
