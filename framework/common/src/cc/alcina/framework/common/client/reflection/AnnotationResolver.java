package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;

public interface AnnotationResolver {
	 <A extends Annotation> A getAnnotation(Class<A> annotationClass) ;

	default <A extends Annotation> boolean hasAnnotation(Class<A> annotationClass){
		return getAnnotation(annotationClass)!=null;
	}
}
