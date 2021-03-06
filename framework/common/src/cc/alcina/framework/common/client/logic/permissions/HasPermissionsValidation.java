package cc.alcina.framework.common.client.logic.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer.SerializerOptions;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface HasPermissionsValidation {
	public String validatePermissions();

	public static class DefaultValidation {
		public static boolean validateWithFlatTreeSerializer = false;

		public static String validatePermissions(HasPermissionsValidation hpv,
				Collection children) {
			if (hpv instanceof Permissible) {
				Permissible permissible = (Permissible) hpv;
				if (!PermissionsManager.get().isPermitted(hpv, permissible)) {
					return "Access not permitted to "
							+ CommonUtils.simpleClassName(hpv.getClass());
				}
			}
			TypeSerialization typeSerialization = Reflections.classLookup()
					.getAnnotationForClass(hpv.getClass(),
							TypeSerialization.class);
			if (typeSerialization != null && validateWithFlatTreeSerializer) {
				// serialization-with-test checks valid type membership
				SerializerOptions options = new FlatTreeSerializer.SerializerOptions()
						.withTestSerialized(true).withTopLevelTypeInfo(true);
				FlatTreeSerializer.serialize((TreeSerializable) hpv, options);
				return null;
			}
			List<Class> permissibleChildClasses = new ArrayList<Class>();
			PermissibleChildClasses pcc = Reflections.classLookup()
					.getAnnotationForClass(hpv.getClass(),
							PermissibleChildClasses.class);
			if (pcc != null) {
				permissibleChildClasses = Arrays.asList(pcc.value());
			}
			for (Object o : children) {
				if (!permissibleChildClasses.contains(o.getClass())) {
					return Ax.format(
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
