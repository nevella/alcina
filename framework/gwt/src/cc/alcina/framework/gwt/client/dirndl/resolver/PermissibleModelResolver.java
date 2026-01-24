package cc.alcina.framework.gwt.client.dirndl.resolver;

import java.util.Map;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class PermissibleModelResolver extends ContextResolver {
	PermissibleModelResolver.Support support;

	public PermissibleModelResolver() {
		support = new Support(this::resolveModelSuper,
				this::resolveDirectedProperty0Super);
		resolveDirectedPropertyAscends = false;
	}

	protected Object resolveModelSuper(Node parentNode,
			AnnotationLocation location, Object model) {
		return super.resolveModel(parentNode, location, model);
	}

	protected Property resolveDirectedProperty0Super(Property property) {
		return super.resolveDirectedProperty0(property);
	}

	@Override
	protected Object resolveModel(Node parentNode, AnnotationLocation location,
			Object model) {
		return support.resolveModel(parentNode, location, model);
	}

	@Override
	protected Property resolveDirectedProperty0(Property property) {
		return support.resolveDirectedProperty0(property);
	}

	/*
	 * Factors out the guts so the behaviour can be reused in a mixin
	 */
	public static class Support {
		Map<Class, Permission> classAccessPermissions = AlcinaCollections
				.newUnqiueMap();

		Map<Property, Permission> propertyVisibilityPermissions = AlcinaCollections
				.newUnqiueMap();

		ContextResolver.ModelResolver modelResolver;

		ContextResolver.DirectedPropertyResolver directedPropertyResolver;

		public Support(ContextResolver.ModelResolver modelResolver,
				ContextResolver.DirectedPropertyResolver directedPropertyResolver) {
			this.modelResolver = modelResolver;
			this.directedPropertyResolver = directedPropertyResolver;
		}

		public Property resolveDirectedProperty0(Property property) {
			Permission propertyVisible = propertyVisibilityPermissions
					.computeIfAbsent(property, prop -> prop
							.annotationOptional(UiPermission.Visible.class)
							.map(UiPermission.Visible::value).orElse(null));
			if (propertyVisible != null
					&& !Permissions.isPermitted(propertyVisible)) {
				return null;
			}
			return directedPropertyResolver.resolveDirectedProperty0(property);
		}

		public Object resolveModel(Node parentNode, AnnotationLocation location,
				Object model) {
			if (model == null) {
				return modelResolver.resolveModel(parentNode, location, model);
			}
			Class<? extends Object> modelClass = model.getClass();
			Permission classAccess = classAccessPermissions.computeIfAbsent(
					modelClass,
					clazz -> Reflections.at(clazz)
							.annotationOptional(UiPermission.Access.class)
							.map(UiPermission.Access::value).orElse(null));
			if (classAccess != null
					&& !Permissions.isPermitted(model, classAccess)) {
				return new AccessDenied(model);
			}
			return modelResolver.resolveModel(parentNode, location, model);
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
