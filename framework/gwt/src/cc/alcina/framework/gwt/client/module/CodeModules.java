package cc.alcina.framework.gwt.client.module;

import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CollectionCreators;

@Reflected
@Registration.Singleton
// FIXME - client.module
public class CodeModules {
	public static CodeModules get() {
		return Registry.impl(CodeModules.class);
	}

	Map<Class, Boolean> registered = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	public boolean isRegistered(Class<?> clazz) {
		return registered.containsKey(clazz);
	}

	public void register(Class<?> clazz) {
		registered.put(clazz, true);
	}
}
