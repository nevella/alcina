package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;

public interface HasAnnotations {
	<A extends Annotation> A annotation(Class<A> annotationClass);
}
