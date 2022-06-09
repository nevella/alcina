package cc.alcina.framework.common.client.logic.reflection.resolution;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class AnnotationLocation {
	public Property property;

	/*
	 * Can be either the containing class of the property, or the value type of
	 * the property Í
	 */
	public Class<?> classLocation;

	public Resolver resolver;

	public AnnotationLocation(Class clazz, Property property) {
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

	protected AnnotationLocation() {
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnnotationLocation) {
			AnnotationLocation o = (AnnotationLocation) obj;
			return property == o.property && classLocation == o.classLocation
					&& Objects.equals(resolver, o.resolver);
		} else {
			return false;
		}
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return resolver.resolveAnnotation(annotationClass, this);
	}

	public <A extends Annotation> List<A>
			getAnnotations(Class<A> annotationClass) {
		return resolver.resolveAnnotations(annotationClass, this);
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
		return property != null && property.getDefiningType() == clazz;
	}

	public boolean isPropertyName(String propertyName) {
		return property != null && propertyName.equals(property.getName());
	}

	public boolean isPropertyType(Class<?> clazz) {
		return property != null && clazz == property.getType();
	}

	public AnnotationLocation parent() {
		if (property != null) {
			if (classLocation != null) {
				return new AnnotationLocation(classLocation, null, resolver);
			} else {
				return null;
			}
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
			String declaringPrefix = property.getDefiningType() == classLocation
					? ""
					: Ax.format("(%s)", classLocation.getSimpleName());
			return Ax.format("%s%s", declaringPrefix, property.toString());
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
			ClassReflector<?> classReflector = Reflections.at(classLocation);
			if (classReflector == null) {
				return null;
			}
			return (A) classReflector.annotation(annotationClass);
		}
	}

	public static abstract class Resolver {
		private static MultikeyMap<List<? extends Annotation>> resolvedCache = new UnsortedMultikeyMap<>(
				2);

		/*
		 * Must be bootstrapped by imperative code
		 */
		public static AnnotationLocation.Resolver get() {
			return Registry.impl(AnnotationLocation.Resolver.class);
		}

		@Override
		public /*
				 * Make sure this is correct in children
				 */
		boolean equals(Object obj) {
			return obj.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		public synchronized <A extends Annotation> A resolveAnnotation(
				Class<A> annotationClass, AnnotationLocation location) {
			return (A) Ax.first(resolveAnnotations(annotationClass, location));
		}

		public <A extends Annotation> List<A> resolveAnnotations(
				Class<A> annotationClass, AnnotationLocation location) {
			return (List<A>) resolvedCache.ensure(
					() -> resolveAnnotations0(annotationClass, location),
					location, annotationClass);
		}

		/*
		 * Simplistic implementation, doesn't respect @Resolution
		 */
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			A ensured = location.getAnnotation0(annotationClass);
			return ensured == null ? Collections.emptyList()
					: Collections.singletonList(ensured);
		}
	}
}
