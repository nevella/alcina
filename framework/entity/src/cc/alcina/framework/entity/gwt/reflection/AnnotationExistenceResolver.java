package cc.alcina.framework.entity.gwt.reflection;

import java.lang.annotation.Annotation;

import com.google.gwt.core.ext.typeinfo.JClassType;

public interface AnnotationExistenceResolver {
	<A extends Annotation> boolean has(JClassType t,
			Class<A> annotationClass);
}