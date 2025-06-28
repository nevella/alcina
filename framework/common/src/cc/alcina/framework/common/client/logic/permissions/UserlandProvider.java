package cc.alcina.framework.common.client.logic.permissions;

import java.util.Objects;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class UserlandProvider {
	public static UserlandProvider get() {
		return Registry.impl(UserlandProvider.class);
	}

	private Entity systemUser;

	private Entity anonymousUser;

	public <U extends Entity & IUser> U getAnonymousUser() {
		if (anonymousUser == null) {
			anonymousUser = getUserByName(Permissions.Names.ANONYMOUS_USER);
		}
		return (U) anonymousUser;
	}

	public <G extends Entity & IGroup> G getGroupByName(String name) {
		return (G) Domain.by(
				(Class<G>) PersistentImpl.getImplementation(IGroup.class),
				"name", name);
	}

	public <U extends Entity & IUser> U getSystemUser() {
		if (systemUser == null) {
			systemUser = getUserByName(Permissions.Names.SYSTEM_USER);
		}
		return (U) systemUser;
	}

	public <U extends Entity & IUser> U getUserById(Long id) {
		return (U) Domain.find(
				(Class<U>) PersistentImpl.getImplementation(IUser.class), id);
	}

	public <U extends Entity & IUser> U getUserByName(String name) {
		return (U) Domain.by(
				(Class<U>) PersistentImpl.getImplementation(IUser.class),
				"userName", name);
	}

	public boolean isSystemUser(IUser user) {
		return Objects.equals(user,
				(IUser) getUserByName(Permissions.Names.SYSTEM_USER));
	}
}
