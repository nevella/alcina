package cc.alcina.framework.gwt.client.dirndl.resolver;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class PermissibleModelResolver extends ContextResolver {
	PermissibleModelResolver.Support support;

	public PermissibleModelResolver() {
		support = new Support(super::resolveModel);
	}

	@Override
	protected Object resolveModel(AnnotationLocation location, Object model) {
		return support.resolveModel(location, model);
	}

	/*
	 * Factors out the guts so the behaviour can be reused in a mixin
	 */
	public static class Support {
		Map<Class, Boolean> hasPermissions = AlcinaCollections.newUnqiueMap();

		BiFunction<AnnotationLocation, Object, Object> superResolveModel;

		public Support(
				BiFunction<AnnotationLocation, Object, Object> superResolveModel) {
			this.superResolveModel = superResolveModel;
		}

		public Object resolveModel(AnnotationLocation location, Object model) {
			if (model == null) {
				return superResolveModel.apply(location, model);
			}
			Class<? extends Object> modelClass = model.getClass();
			boolean hasPermission = hasPermissions.computeIfAbsent(modelClass,
					clazz -> Reflections.at(clazz).has(Permission.class));
			if (!hasPermission) {
				return model;
			}
			Permission permission = Reflections.at(modelClass)
					.annotation(Permission.class);
			if (!Permissions.isPermitted(model)) {
				return new AccessDenied(model);
			}
			return superResolveModel.apply(location, model);
		}
	}

	@Directed
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
