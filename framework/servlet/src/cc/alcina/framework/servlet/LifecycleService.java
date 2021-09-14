package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrySingleton;

@RegistryLocation(registryPoint = LifecycleService.class)
public abstract class LifecycleService implements RegistrySingleton {
	public void onApplicationShutdown() {
	}

	public void onApplicationStartup() {
	}
}
