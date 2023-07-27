package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
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
		public Map<Class, Annotation> annotations = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return (A) annotations.get(annotationClass);
		}

		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			throw new UnsupportedOperationException();
		}
	}
}
