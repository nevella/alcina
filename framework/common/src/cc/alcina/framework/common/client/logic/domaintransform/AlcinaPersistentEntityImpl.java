package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.WrappedRuntimeException;
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
	static <A> Class<? extends A> getImplementation(Class<A> clazz) {
		return Registry.get().lookupSingle(AlcinaPersistentEntityImpl.class,
				clazz);
	}

	static String getImplementationSimpleClassName(Class<?> clazz) {
		return getImplementation(clazz).getSimpleName();
	}

	static <A> A getNewImplementationInstance(Class<A> clazz) {
		try {
			return getImplementation(clazz).newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
