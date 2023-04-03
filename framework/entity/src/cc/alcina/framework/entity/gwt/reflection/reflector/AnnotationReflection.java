package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;

/**
 * Because GWT reuses JDK annotations, this is just an {@link Annotation}
 * instance holder
 */
public class AnnotationReflection extends ReflectionElement
		implements Comparable<AnnotationReflection> {
	private final Annotation annotation;

	public AnnotationReflection(Annotation annotation) {
		this.annotation = annotation;
	}

	@Override
	public int compareTo(AnnotationReflection o) {
		return annotation.annotationType().getCanonicalName()
				.compareTo(o.annotation.annotationType().getCanonicalName());
	}

	public Annotation getAnnotation() {
		return this.annotation;
	}

	@Override
	public void prepare() {
		throw new UnsupportedOperationException();
	}
}
