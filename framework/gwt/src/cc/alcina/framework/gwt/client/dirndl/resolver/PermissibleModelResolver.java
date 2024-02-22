package cc.alcina.framework.gwt.client.dirndl.resolver;

import java.util.Map;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class PermissibleModelResolver extends ContextResolver {
	Map<Class, Boolean> hasPermissions = AlcinaCollections.newUnqiueMap();

	@Override
	protected Object resolveModel(Object model) {
		if (model == null) {
			return super.resolveModel(model);
		}
		Class<? extends Object> modelClass = model.getClass();
		boolean hasPermission = hasPermissions.computeIfAbsent(modelClass,
				clazz -> Reflections.at(clazz).has(Permission.class));
		if (!hasPermission) {
			return model;
		}
		Permission permission = Reflections.at(modelClass)
				.annotation(Permission.class);
		if (!PermissionsManager.isPermitted(model)) {
			return new AccessDenied(model);
		}
		return super.resolveModel(model);
	}

	@Directed(tag = "acccess-denied")
	static class AccessDenied extends Model.All
			implements Model.ResetDirecteds {
		AccessDenied(Object model) {
			FormatBuilder format = new FormatBuilder().separator(" :: ");
			format.append("Access denied");
			if (GWT.isClient() && !GWT.isScript()) {
				format.append("(dev mode)");
				format.format("failed permissions check on %s",
						model.getClass());
			}
			message = format.toString();
		}

		String message;
	}
}
