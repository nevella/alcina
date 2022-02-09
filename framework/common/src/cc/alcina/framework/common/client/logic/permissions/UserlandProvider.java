package cc.alcina.framework.common.client.logic.permissions;

import java.util.Objects;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocation(registryPoint = UserlandProvider.class, implementationType = ImplementationType.SINGLETON)
@Registration.Singleton
public class UserlandProvider {

    public static UserlandProvider get() {
        return Registry.impl(UserlandProvider.class);
    }

    public <U extends Entity & IUser> U getAnonymousUser() {
        return getUserByName(PermissionsManager.ANONYMOUS_USER_NAME);
    }

    public <G extends Entity & IGroup> G getGroupByName(String name) {
        return (G) Domain.by((Class<G>) PersistentImpl.getImplementation(IGroup.class), "name", name);
    }

    public <U extends Entity & IUser> U getSystemUser() {
        return getUserByName(PermissionsManager.SYSTEM_USER_NAME);
    }

    public <U extends Entity & IUser> U getUserById(Long id) {
        return (U) Domain.find((Class<U>) PersistentImpl.getImplementation(IUser.class), id);
    }

    public <U extends Entity & IUser> U getUserByName(String name) {
        return (U) Domain.by((Class<U>) PersistentImpl.getImplementation(IUser.class), "userName", name);
    }

    public boolean isSystemUser(IUser user) {
        return Objects.equals(user, (IUser) getUserByName(PermissionsManager.SYSTEM_USER_NAME));
    }
}
