package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;

import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.reflection.Property;

public interface ReflectionVisibility {
	boolean isVisibleAnnotation(Class<? extends Annotation> annotationType);

	default boolean isVisibleField(JField field) {
		return !field.isTransient() && !field.isPrivate() && !field.isStatic()
				&& !field.isAnnotationPresent(Property.Not.class);
	}

	default boolean isVisibleMethod(JMethod method) {
		// follow beans/introspector behaviour
		if (method.getName().equals("getPropertyChangeListeners")
				&& method.getParameters().length == 0) {
			return false;
		}
		if (method.isAnnotationPresent(Property.Not.class)) {
			return false;
		}
		return true;
	}

	boolean isVisibleType(JType type);
}
