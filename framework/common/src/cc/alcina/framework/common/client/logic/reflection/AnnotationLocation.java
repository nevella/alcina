package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.Objects;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class AnnotationLocation {
	public Property property;

	/*
	 * Can be either the containing class of the property, or the value
	 * type of the property reflector
	 */
	public Class classLocation;

	public Resolver resolver;

	public AnnotationLocation(Class clazz,
			Property property) {
		this(clazz, property, null);
	}

	public AnnotationLocation(Class clazz, Property property,
			Resolver resolver) {
		Preconditions.checkArgument(clazz != null || property != null);
		this.classLocation = clazz;
		this.property = property;
		if (resolver == null) {
			resolver = Resolver.get();
		}
		this.resolver = resolver;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnnotationLocation) {
			AnnotationLocation o = (AnnotationLocation) obj;
			return property == o.property
					&& classLocation == o.classLocation
					&& Objects.equals(resolver, o.resolver);
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
		return Objects.hash(property, classLocation, resolver);
	}

	public boolean isDefiningType(Class<?> clazz) {
		return property != null
				&& property.getDefiningType() == clazz;
	}

	public boolean isPropertyName(String propertyName) {
		return property != null
				&& propertyName.equals(property.getName());
	}

	public boolean isPropertyType(Class<?> clazz) {
		return property != null
				&& clazz == property.getType();
	}

	public AnnotationLocation parent() {
		if (property != null) {
			return new AnnotationLocation(classLocation, null, resolver);
		}
		if (classLocation.getSuperclass() != null) {
			return new AnnotationLocation(classLocation.getSuperclass(), null,
					resolver);
		}
		return null;
	}

	@Override
	public String toString() {
		if (property != null) {
			String declaringPrefix = property
					.getDefiningType() == classLocation ? ""
							: Ax.format("(%s)", classLocation.getSimpleName());
			return Ax.format("%s%s", declaringPrefix,
					property.toString());
		} else {
			return classLocation.getSimpleName();
		}
	}

	private <A extends Annotation> A getAnnotation0(Class<A> annotationClass) {
		if (property != null) {
			A annotation = property.annotation(annotationClass);
			if (annotation != null) {
				return annotation;
			}
		}
		if (classLocation == null) {
			return null;
		} else {
			return (A) Reflections.at(classLocation).annotation(annotationClass);
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
			Annotation ensure = resolvedCache.ensure(
					() -> resolveAnnotation0(annotationClass, location),
					location, annotationClass);
			return (A) ensure;
		}

		protected <A extends Annotation> Annotation resolveAnnotation0(
				Class<A> annotationClass, AnnotationLocation location) {
			A ensured = location.getAnnotation0(annotationClass);
			return ensured;
		}
	}
}