package cc.alcina.framework.common.client.logic.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface HasPermissionsValidation {
	public String validatePermissions();

	public static class DefaultValidation {
		public static String validatePermissions(HasPermissionsValidation hpv,
				Collection children) {
			if (hpv instanceof Permissible) {
				Permissible permissible = (Permissible) hpv;
				if (!PermissionsManager.get().isPermissible(hpv,permissible)) {
					return "Access not permitted to "
							+ CommonUtils.simpleClassName(hpv.getClass());
				}
			}
			List<Class> permissibleChildClasses = new ArrayList<Class>();
			PermissibleChildClasses pcc = CommonLocator.get().classLookup()
					.getAnnotationForClass(hpv.getClass(),
							PermissibleChildClasses.class);
			if (pcc != null) {
				permissibleChildClasses = Arrays.asList(pcc
						.value());
			}
			for (Object o : children) {
				if (!permissibleChildClasses.contains(o.getClass())) {
					return CommonUtils.formatJ(
							"Access not permitted (per-class): %s, child %s",
							CommonUtils.simpleClassName(hpv.getClass()),
							CommonUtils.simpleClassName(o.getClass()));
				}
				if (o instanceof HasPermissionsValidation) {
					HasPermissionsValidation childHpv = (HasPermissionsValidation) o;
					String validationResult = childHpv.validatePermissions();
					if (validationResult != null) {
						return validationResult;
					}
				}
			}
			return null;
		}
	}
}
