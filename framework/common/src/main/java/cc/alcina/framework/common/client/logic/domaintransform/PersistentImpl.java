package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * maaarrrkkkerrrr
 *
 * 
 *
 */
public interface PersistentImpl {
	static <A extends Entity> A create(Class<A> clazz) {
		return Domain.create(getImplementation(clazz));
	}

	static <A extends Entity> A ensure(Class<A> clazz, String propertyName,
			Object value) {
		return Domain.ensure(getImplementation(clazz), propertyName, value);
	}

	static <A extends Entity> A find(Class<A> clazz, Long id) {
		return Domain.find(getImplementation(clazz), id);
	}

	static <A> Class<? extends A> getImplementation(Class<A> clazz) {
		return Registry.query(clazz).setKeys(PersistentImpl.class, clazz)
				.registration();
	}

	static <A> Class<? extends A> getImplementationOrSelf(Class<A> clazz) {
		return hasImplementation(clazz) ? getImplementation(clazz) : clazz;
	}

	static String getImplementationSimpleClassName(Class<?> clazz) {
		return getImplementation(clazz).getSimpleName();
	}

	static <A> A getNewImplementationInstance(Class<A> clazz) {
		return Reflections.newInstance(getImplementation(clazz));
	}

	static boolean hasImplementation(Class<?> clazz) {
		return Registry.query().addKeys(PersistentImpl.class, clazz)
				.untypedRegistrations().count() > 0;
	}
}
