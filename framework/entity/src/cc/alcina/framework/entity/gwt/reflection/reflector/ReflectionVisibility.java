package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;

import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

public interface ReflectionVisibility {
	boolean isVisibleAnnotation(Class<? extends Annotation> annotationType);

	default boolean isVisibleMethod(JMethod method) {
		// follow beans/introspector behaviour
		if (method.getName().equals("getPropertyChangeListeners")
				&& method.getParameters().length == 0) {
			return false;
		}
		return true;
	}

	boolean isVisibleType(JType type);
}
