package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

public interface HasAnnotationCallback<A extends Annotation> {
	public void apply(A annotation, PropertyReflector propertyReflector);
}