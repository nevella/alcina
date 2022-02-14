package cc.alcina.framework.entity.logic.permissions;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public abstract class ThreadedPmClientInstanceResolver {
	public static synchronized ThreadedPmClientInstanceResolver get() {
		return Registry.impl(ThreadedPmClientInstanceResolver.class);
	}

	public abstract ClientInstance getClientInstance();

	public abstract Long getClientInstanceId();
}
