package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrySingleton;

@RegistryLocation(registryPoint = LifecycleService.class)
@Registration(LifecycleService.class)
public abstract class LifecycleService implements RegistrySingleton {
	public LifecycleService() {
		
	}

	public void onApplicationShutdown() {
	}

	public void onApplicationStartup() {
	}
}
