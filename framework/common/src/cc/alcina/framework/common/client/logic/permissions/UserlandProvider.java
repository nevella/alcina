package cc.alcina.framework.common.client.logic.permissions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = UserlandProvider.class, implementationType = ImplementationType.SINGLETON)
public class UserlandProvider {
	public static UserlandProvider get() {
		return Registry.impl(UserlandProvider.class);
	}

	public <U extends Entity & IUser> U getAnonymousUser() {
		return getUserByName(PermissionsManager.ANONYMOUS_USER_NAME);
	}

	public <G extends Entity & IGroup> G getGroupByName(String name) {
		return (G) Domain.byProperty((Class<G>) AlcinaPersistentEntityImpl
				.getImplementation(IGroup.class), "name", name);
	}

	public <U extends Entity & IUser> U getSystemUser() {
		return getUserByName(PermissionsManager.SYSTEM_USER_NAME);
	}

	public <U extends Entity & IUser> U getUserByName(String name) {
		return (U) Domain
				.byProperty(
						(Class<U>) AlcinaPersistentEntityImpl
								.getImplementation(IUser.class),
						"userName", name);
	}
}
