package cc.alcina.framework.common.client.logic.permissions;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsExtension;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
/*
 * T will be the permissions *target* (not the verb aka permissible)
 */
public abstract class Rule<T> implements PermissionsExtension {
	@Override
	public Boolean isPermitted(Object o, Permissible p) {
		return isPermittedTyped((T) o, null, p);
	}

	public abstract Boolean isPermittedTyped(T target, Object assigningTo,
			Permissible permissible);
}