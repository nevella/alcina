package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

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
						: Reflections.classLookup().getPropertyReflector(clazz,
								propertyName)).getAnnotation(annotationClass);
	}

	public static <A extends Annotation> A resolve(PropertyReflector reflector,
			Class<A> annotationClass) {
		return resolve(reflector, annotationClass, null);
	}

	public static <A extends Annotation> A resolve(PropertyReflector reflector,
			Class<A> annotationClass, AnnotationLocation.Resolver resolver) {
		return new AnnotationLocation(reflector.getDefiningType(), reflector,
				resolver).getAnnotation(annotationClass);
	}
}
