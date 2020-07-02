package cc.alcina.framework.common.client.logic.reflection;

import java.util.Objects;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;

public abstract class PermissionRule<E extends Entity> {
	public abstract E checkPermission(E e) throws PermissionsException;

	protected void throwIfFalse(boolean result) throws PermissionsException {
		if (!result) {
			throw new PermissionsException();
		}
	}

	public static class PermissionRule_Create<E extends Entity>
			extends PermissionRule<E> {
		@Override
		public E checkPermission(E e) throws PermissionsException {
			ObjectPermissions objectPermissions = Reflections.classLookup()
					.getAnnotationForClass(e.getClass(),
							ObjectPermissions.class);
			throwIfFalse(PermissionsManager.get().isPermissible(e,
					objectPermissions.create()));
			return e;
		}
	}

	public static class PermissionRule_Delete<E extends Entity>
			extends PermissionRule<E> {
		@Override
		public E checkPermission(E e) throws PermissionsException {
			ObjectPermissions objectPermissions = Reflections.classLookup()
					.getAnnotationForClass(e.getClass(),
							ObjectPermissions.class);
			throwIfFalse(PermissionsManager.get().isPermissible(e,
					objectPermissions.delete()));
			return e;
		}
	}

	public static class PermissionRule_IsGroupMember<E extends Entity & IGroup>
			extends PermissionRule<E> {
		@Override
		public E checkPermission(E e) throws PermissionsException {
			throwIfFalse(e.containsUser(PermissionsManager.get().getUser()));
			return e;
		}
	}

	public static class PermissionRule_IsOwner<E extends Entity & HasOwner>
			extends PermissionRule<E> {
		@Override
		public E checkPermission(E e) throws PermissionsException {
			throwIfFalse(Objects.equals(e.getOwner(),
					PermissionsManager.get().getUser()));
			return e;
		}
	}

	public static class PermissionRule_Read<E extends Entity>
			extends PermissionRule<E> {
		@Override
		public E checkPermission(E e) throws PermissionsException {
			ObjectPermissions objectPermissions = Reflections.classLookup()
					.getAnnotationForClass(e.getClass(),
							ObjectPermissions.class);
			throwIfFalse(PermissionsManager.get().isPermissible(e,
					objectPermissions.read()));
			return e;
		}
	}

	public static class PermissionRule_Write<E extends Entity>
			extends PermissionRule<E> {
		@Override
		public E checkPermission(E e) throws PermissionsException {
			ObjectPermissions objectPermissions = Reflections.classLookup()
					.getAnnotationForClass(e.getClass(),
							ObjectPermissions.class);
			throwIfFalse(PermissionsManager.get().isPermissible(e,
					objectPermissions.write()));
			return e;
		}
	}
}
