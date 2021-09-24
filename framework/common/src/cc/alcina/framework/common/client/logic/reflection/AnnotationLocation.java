package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.Objects;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

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
		Preconditions.checkArgument(clazz != null || propertyReflector != null);
		this.classLocation = clazz;
		this.propertyReflector = propertyReflector;
		this.resolver = resolver;
		this.fallbackToClassLocation = fallbackToClassLocation;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnnotationLocation) {
			AnnotationLocation o = (AnnotationLocation) obj;
			return propertyReflector == o.propertyReflector
					&& classLocation == o.classLocation
					&& Objects.equals(resolver, o.resolver)
					&& fallbackToClassLocation == o.fallbackToClassLocation;
		} else {
			return false;
		}
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return resolver.resolveAnnotation(annotationClass, this);
	}

	public <A extends Annotation> boolean
			hasAnnotation(Class<A> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertyReflector, classLocation, resolver,
				fallbackToClassLocation);
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
			if (classLocation == null) {
				int debug = 3;
			}
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
	public static class DefaultResolver extends Resolver {
	}

	public static abstract class Resolver {
		private static MultikeyMap<Annotation> resolvedCache = new UnsortedMultikeyMap<>(
				2);

		public static AnnotationLocation.Resolver get() {
			return Registry.impl(AnnotationLocation.Resolver.class);
		}

		@Override
		/*
		 * Make sure this is correct in children
		 */
		public boolean equals(Object obj) {
			return obj.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		public synchronized <A extends Annotation> A resolveAnnotation(
				Class<A> annotationClass, AnnotationLocation location) {
			Annotation ensure = resolvedCache.ensure(() -> {
				A ensured = location.getAnnotation0(annotationClass);
				return ensured;
			}, location, annotationClass);
			return (A) ensure;
		}
	}
}