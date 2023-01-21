package cc.alcina.framework.entity.logic.permissions;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public abstract class ThreadedPmClientInstanceResolver {
	private static ThreadedPmClientInstanceResolver instance;
	// optimise, save the instance

	public static ThreadedPmClientInstanceResolver get() {
		// synchronization :: rely on registry to return same impl
		if (instance == null) {
			instance = Registry.impl(ThreadedPmClientInstanceResolver.class);
		}
		return instance;
	}

	public abstract ClientInstance getClientInstance();

	public abstract Long getClientInstanceId();
}
