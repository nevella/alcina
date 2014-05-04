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
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;

public class PermissibleFieldFilter implements GraphProjectionFieldFilter {
	public static boolean disablePerObjectPermissions;

	public <T> T filterData(T original, T projected,
			GraphProjectionContext context, GraphProjection graphProjection)
			throws Exception {
		return null;
	}

	@Override
	public Boolean permitClass(Class clazz) {
		ObjectPermissions op = (ObjectPermissions) clazz
				.getAnnotation(ObjectPermissions.class);
		return permit(clazz, op == null ? null : op.read(),null);
	}

	public boolean permitField(Field field,
			Set<Field> perObjectPermissionFields, Class forClass) {
		try {
			Class<?> type = field.getType();
			Class<?> checkType = field.getType();
			if (!GraphProjection.isPrimitiveOrDataClass(type)) {
				if(shallow(forClass)){
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
			Boolean permit = permit(forClass, pp == null ? null : pp.read(),field);
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

	protected boolean shallow(Class<?> type) {
		return false;
	}

	protected Boolean permit(Class clazz, Permission permission, Field field) {
		if (permission != null) {
			AnnotatedPermissible ap = new AnnotatedPermissible(permission);
			if (disablePerObjectPermissions) {
				return true;
				// only in app startup/warmup
			}
			if (PermissionsManager.get().isPermissible(null, ap, true)) {
				return true;
			}
			if (ap.accessLevel().ordinal() <= AccessLevel.GROUP.ordinal()) {
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

	@Override
	public boolean permitTransient(Field field) {
		return false;
	}
}