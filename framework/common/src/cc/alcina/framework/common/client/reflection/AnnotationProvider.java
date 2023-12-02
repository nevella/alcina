package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CollectionCreators;

public interface AnnotationProvider {
	<A extends Annotation> A getAnnotation(Class<A> annotationClass);

	<A extends Annotation> List<A> getAnnotations(Class<A> annotationClass);

	default <A extends Annotation> boolean
			hasAnnotation(Class<A> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	public static class LookupProvider implements AnnotationProvider {
		Map<Class, Annotation> annotations = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		Map<Class, List<Annotation>> repeatedAnnotations = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		public synchronized void addAnnotation(
				Class<? extends Annotation> annotationClass,
				Annotation annotation) {
			annotations.put(annotationClass, annotation);
			repeatedAnnotations
					.computeIfAbsent(annotationClass, clazz -> new ArrayList())
					.add(annotation);
		}

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return (A) annotations.get(annotationClass);
		}

		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			return (List<A>) repeatedAnnotations.get(annotationClass);
		}
	}

	public static class EmptyProvider implements AnnotationProvider {
		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return null;
		}

		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			return List.of();
		}
	}
}
