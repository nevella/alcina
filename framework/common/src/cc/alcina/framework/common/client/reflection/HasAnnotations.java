package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.List;

public interface HasAnnotations {
	<A extends Annotation> A annotation(Class<A> annotationClass);

	<A extends Annotation> List<A> annotations(Class<A> annotationClass);

	boolean isProperty(Class<?> owningType, String propertyName);

	default Property asProperty() {
		return (Property) this;
	}
}
