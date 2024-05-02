package cc.alcina.framework.entity.gwt.reflection.reflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CachingToStringComparator;
import cc.alcina.framework.common.client.util.CommonUtils;

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

	public Stream<Class> getAnnotationAttributeTypes() {
		Class<? extends Annotation> annotationType = annotation
				.annotationType();
		List<Method> declaredMethods = new ArrayList<Method>(
				Arrays.asList(annotationType.getDeclaredMethods()));
		Collections.sort(declaredMethods, new CachingToStringComparator());
		Set<Class> types = new LinkedHashSet<>();
		try {
			for (Method method : declaredMethods) {
				if (method.getName()
						.matches("hashCode|toString|equals|annotationType")) {
					continue;
				}
				Object annotationValue = method.invoke(annotation,
						CommonUtils.EMPTY_OBJECT_ARRAY);
				if (annotationValue.getClass().isArray()) {
					if (annotationValue.getClass().getComponentType()
							.isPrimitive()) {
					} else {
						Arrays.stream((Object[]) annotationValue)
								.filter(o -> o instanceof Class)
								.forEach(o -> types.add((Class) o));
					}
				} else {
					if (annotationValue instanceof Class) {
						types.add((Class) annotationValue);
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return types.stream();
	}

	@Override
	public void prepare() {
		throw new UnsupportedOperationException();
	}
}
