package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;

public class AnnotationLocation {
	public PropertyReflector propertyReflector;

	public Class containingClass;

	public AnnotationLocation(Class containingClass,
			PropertyReflector propertyReflector) {
		this.propertyReflector = propertyReflector;
		this.containingClass = containingClass;
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if (propertyReflector != null) {
			A annotation = propertyReflector.getAnnotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		if (containingClass == null) {
			return null;
		} else {
			return Reflections.classLookup()
					.getAnnotationForClass(containingClass, annotationClass);
		}
	}
}