package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;

public class AnnotationLocation {
	public PropertyReflector propertyReflector;

	/*
	 * Can be either the containing class of the propertyReflector, or the value
	 * type of the property reflector
	 */
	public Class fallbackToClass;

	private Resolver resolver;

	public AnnotationLocation(Class clazz,
			PropertyReflector propertyReflector) {
		this(clazz, propertyReflector, new DefaultResolver());
	}

	public AnnotationLocation(Class clazz, PropertyReflector propertyReflector,
			Resolver resolver) {
		this.fallbackToClass = clazz;
		this.propertyReflector = propertyReflector;
		this.resolver = resolver;
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return resolver.getAnnotation(annotationClass, this);
	}

	@Override
	public String toString() {
		if (propertyReflector != null) {
			return propertyReflector.toString();
		} else {
			return fallbackToClass.getSimpleName();
		}
	}

	private <A extends Annotation> A getAnnotation0(Class<A> annotationClass) {
		if (propertyReflector != null) {
			A annotation = propertyReflector.getAnnotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		if (fallbackToClass == null) {
			return null;
		} else {
			return Reflections.classLookup().getAnnotationForClass(fallbackToClass,
					annotationClass);
		}
	}

	public static class DefaultResolver implements Resolver {
	}

	public static interface Resolver {
		default <A extends Annotation> A getAnnotation(Class<A> annotationClass,
				AnnotationLocation location) {
			return location.getAnnotation0(annotationClass);
		}
	}
}