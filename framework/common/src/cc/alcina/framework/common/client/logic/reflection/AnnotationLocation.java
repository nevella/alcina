package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;

public class AnnotationLocation {
	public PropertyReflector propertyReflector;

	public Class containingClass;

	private Resolver resolver;

	public AnnotationLocation(Class containingClass,
			PropertyReflector propertyReflector) {
		this(containingClass, propertyReflector, new DefaultResolver());
	}

	public AnnotationLocation(Class containingClass,
			PropertyReflector propertyReflector, Resolver resolver) {
		this.containingClass = containingClass;
		this.propertyReflector = propertyReflector;
		this.resolver = resolver;
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return resolver.getAnnotation(annotationClass, this);
	}

	private <A extends Annotation> A getAnnotation0(Class<A> annotationClass) {
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

	@Override
	public String toString() {
		if (propertyReflector != null) {
			return propertyReflector.toString();
		} else {
			return containingClass.getSimpleName();
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