package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.HasOwner;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class PermissibleFieldFilter implements GraphProjectionFieldFilter {
	public static boolean disablePerObjectPermissions;

	private static ThreadLocal<Boolean> disabledPerThreadPerObjectPermissionsThreadLocal = new ThreadLocal() {
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	public static void setDisabledPerThreadPerObjectPermissions(boolean set) {
		disabledPerThreadPerObjectPermissionsThreadLocal.set(set);
	}

	private Boolean disabledPerThreadPerObjectPermissionsInstance;

	CachingMap<Class, ObjectPermissions> objectPermissionLookup = new CachingMap<Class, ObjectPermissions>(
			clazz -> (ObjectPermissions) clazz
					.getAnnotation(ObjectPermissions.class));

	public PermissibleFieldFilter() {
		disabledPerThreadPerObjectPermissionsInstance = disabledPerThreadPerObjectPermissionsThreadLocal
				.get();
	}

	@Override
	public Boolean permitClass(Class clazz) {
		ObjectPermissions op = objectPermissionLookup.get(clazz);
		return permit(clazz, op == null ? null : op.read(), null);
	}

	public boolean permitField(Field field,
			Set<Field> perObjectPermissionFields, Class forClass) {
		try {
			Class<?> type = field.getType();
			Class<?> checkType = field.getType();
			if (!GraphProjection.isPrimitiveOrDataClass(type)) {
				if (shallow(forClass)) {
					return false;
				}
				if (Collection.class.isAssignableFrom(type)) {
					checkType = null;
					Type pt = GraphProjection.getGenericType(field);
					if (pt instanceof ParameterizedType) {
						Type genericType = ((ParameterizedType) pt)
								.getActualTypeArguments()[0];
						if (genericType instanceof Class) {
							checkType = (Class) genericType;
						}
					}
				}
				if (checkType != null) {
					Boolean result = permitClass(checkType);
					if (result != null && result.booleanValue() == false) {
						return false;
					}
				}
			}
			PropertyPermissions pp = GraphProjection
					.getPropertyPermission(field);
			Boolean permit = permit(forClass, pp == null ? null : pp.read(),
					field);
			if (permit == null) {
				perObjectPermissionFields.add(field);
				return true;
			} else {
				return permit;
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public boolean permitTransient(Field field) {
		return false;
	}

	protected Boolean permit(Class clazz, Permission permission, Field field) {
		if (permission != null) {
			AnnotatedPermissible ap = new AnnotatedPermissible(permission);
			if (disablePerObjectPermissions) {
				return true;
				// only in app startup/warmup
			}
			if (disabledPerThreadPerObjectPermissionsInstance) {
				return true;
				// optimisation for clustered transform commit
			}
			if (PermissionsManager.get().isPermissible(null, ap, true)) {
				return true;
			}
			if (ap.accessLevel() == AccessLevel.GROUP) {
				return null;
			}
			if (ap.accessLevel().ordinal() < AccessLevel.GROUP.ordinal()) {
				return false;
			}
			if (ap.accessLevel() == AccessLevel.ADMIN_OR_OWNER) {
				if (ap.rule().length() > 0) {
					return null;
				}
				if (!PermissionsManager.get().isLoggedIn()) {
					return false;
				}
				if (!HasOwner.class.isAssignableFrom(clazz)) {
					return false;
				}
				return null;
			}
			return ap.rule().isEmpty() ? false : null;
		}
		return true;
		// TODO: 3.2 - replace with a call to tltm (should
		// really be tlpm) that checks obj read perms
		// that'll catch find-object stuff as well
	}

	protected boolean shallow(Class<?> type) {
		return false;
	}

	public static class AllFieldsFilter extends PermissibleFieldFilter {
		@Override
		public Boolean permitClass(Class clazz) {
			return true;
		}

		@Override
		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields, Class forClass) {
			return true;
		}

		@Override
		public boolean permitTransient(Field field) {
			return false;
		}
	}
}