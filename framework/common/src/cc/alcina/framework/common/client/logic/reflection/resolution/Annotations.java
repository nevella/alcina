package cc.alcina.framework.common.client.logic.reflection.resolution;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

public class Annotations {
	public static <A extends Annotation> boolean has(Class clazz,
			String propertyName, Class<A> annotationClass) {
		return resolve(clazz, propertyName, annotationClass) != null;
	}

	public static <A extends Annotation> A resolve(Class clazz,
			Class<A> annotationClass) {
		return resolve(clazz, null, annotationClass);
	}

	public static <A extends Annotation> A resolve(Class clazz,
			String propertyName, Class<A> annotationClass) {
		return new AnnotationLocation(clazz,
				propertyName == null ? null
						: Reflections.at(clazz).property(propertyName))
								.getAnnotation(annotationClass);
	}

	public static <A extends Annotation> A resolve(Property property,
			Class<A> annotationClass) {
		return resolve(property, annotationClass, null);
	}

	public static <A extends Annotation> A resolve(Property property,
			Class<A> annotationClass, AnnotationLocation.Resolver resolver) {
		return new AnnotationLocation(property.getDefiningType(), property,
				resolver).getAnnotation(annotationClass);
	}

	public static <A extends Annotation> List<A> resolveMultiple(Class clazz,
			Class<A> annotationClass) {
		return new AnnotationLocation(clazz, null, null)
				.getAnnotations(annotationClass);
	}
}
