package cc.alcina.framework.common.client.logic.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer.SerializerOptions;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
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
			TypeSerialization typeSerialization = Reflections.at(hpv)
					.annotation(TypeSerialization.class);
			if (typeSerialization != null && validateWithFlatTreeSerializer
					&& typeSerialization.flatSerializable()) {
				// serialization-with-test checks valid type membership
				SerializerOptions options = new FlatTreeSerializer.SerializerOptions()
						.withTestSerialized(true).withTopLevelTypeInfo(true);
				try {
					FlatTreeSerializer.serialize((TreeSerializable) hpv,
							options);
				} catch (RuntimeException e) {
					Ax.err("HasPermissionsValidation exception (unequal serialized): %s",
							AlcinaBeanSerializer.serializeHolder(hpv));
					e.printStackTrace();
				}
				return null;
			}
			List<Class> permissibleChildClasses = new ArrayList<Class>();
			PermissibleChildClasses pcc = Reflections.at(hpv)
					.annotation(PermissibleChildClasses.class);
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
