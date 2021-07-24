package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class AnnotationLocation {
	public PropertyReflector propertyReflector;

	/*
	 * Can be either the containing class of the propertyReflector, or the value
	 * type of the property reflector
	 */
	public Class classLocation;

	private Resolver resolver;

	private boolean fallbackToClassLocation;

	public AnnotationLocation(Class clazz,
			PropertyReflector propertyReflector) {
		this(clazz, propertyReflector, Resolver.get(), true);
	}

	public AnnotationLocation(Class clazz, PropertyReflector propertyReflector,
			Resolver resolver) {
		this(clazz, propertyReflector, resolver, true);
	}

	public AnnotationLocation(Class clazz, PropertyReflector propertyReflector,
			Resolver resolver, boolean fallbackToClassLocation) {
		this.classLocation = clazz;
		this.propertyReflector = propertyReflector;
		this.resolver = resolver;
		this.fallbackToClassLocation = fallbackToClassLocation;
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return resolver.resolveAnnotation(annotationClass, this);
	}

	public <A extends Annotation> boolean
			hasAnnotation(Class<A> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	public boolean isPropertyName(String propertyName) {
		return propertyReflector != null
				&& propertyName.equals(propertyReflector.getPropertyName());
	}

	public boolean isPropertyType(Class<?> clazz) {
		return propertyReflector != null
				&& clazz == propertyReflector.getPropertyType();
	}

	@Override
	public String toString() {
		if (propertyReflector != null) {
			return propertyReflector.toString();
		} else {
			return classLocation.getSimpleName();
		}
	}

	private <A extends Annotation> A getAnnotation0(Class<A> annotationClass) {
		if (propertyReflector != null) {
			A annotation = propertyReflector.getAnnotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		if (classLocation == null || !fallbackToClassLocation) {
			return null;
		} else {
			return Reflections.classLookup()
					.getAnnotationForClass(classLocation, annotationClass);
		}
	}

	@RegistryLocation(registryPoint = Resolver.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class DefaultResolver implements Resolver {
	}

	public static interface Resolver {
		public static AnnotationLocation.Resolver get() {
			return Registry.impl(AnnotationLocation.Resolver.class);
		}

		default <A extends Annotation> A resolveAnnotation(
				Class<A> annotationClass, AnnotationLocation location) {
			return location.getAnnotation0(annotationClass);
		}
	}
}