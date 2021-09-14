package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrySingleton;

@RegistryLocation(registryPoint = LifecycleService.class)
/*
 * Subclasses *must* have a singleton registry implementation
 */
public abstract class LifecycleService implements RegistrySingleton {
	public LifecycleService() {
		Registry.checkSingleton(this);
	}

	public void onApplicationShutdown() {
	}

	public void onApplicationStartup() {
	}
}
