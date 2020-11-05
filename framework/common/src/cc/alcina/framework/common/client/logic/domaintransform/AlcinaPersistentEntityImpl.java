package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.NonClientRegistryPointType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * maaarrrkkkerrrr
 * 
 * @author nick@alcina.cc
 * 
 */
@NonClientRegistryPointType
public interface AlcinaPersistentEntityImpl {
	static <A extends Entity> A create(Class<A> clazz) {
		return Domain.create(getImplementation(clazz));
	}

	static <A extends Entity> A find(Class<A> clazz, Long id) {
		return Domain.find(getImplementation(clazz), id);
	}

	static <A> Class<? extends A> getImplementation(Class<A> clazz) {
		return Registry.get().lookupSingle(AlcinaPersistentEntityImpl.class,
				clazz);
	}

	static String getImplementationSimpleClassName(Class<?> clazz) {
		return getImplementation(clazz).getSimpleName();
	}

	static <A> A getNewImplementationInstance(Class<A> clazz) {
		return Reflections.classLookup().newInstance(getImplementation(clazz));
	}
}
