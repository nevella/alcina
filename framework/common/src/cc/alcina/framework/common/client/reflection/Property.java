package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.CommonUtils;

public class Property {
	private String name;

	private Method getter;

	private Method setter;

	private Class type;

	private Class definingType;

	private AnnotationResolver annotationResolver;

	public Property(String name, Method getter, Method setter,
			Class propertyType, Class definingType,
			AnnotationResolver annotationResolver) {
		this.name = name;
		this.getter = getter;
		this.setter = setter;
		this.type = propertyType;
		this.definingType = definingType;
		this.annotationResolver = annotationResolver;
	}

	public <A extends Annotation> boolean
			hasAnnotation(Class<A> annotationClass) {
		return annotationResolver.hasAnnotation(annotationClass);
	}

	public String getName() {
		return this.name;
	}

	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return annotationResolver.getAnnotation(annotationClass);
	}

	public Class getDefiningType() {
		return definingType;
	}

	public Class getType() {
		return type;
	}

	public Class getType(Object bean) {
		return isWriteOnly() ? getType() : resolveGetter(bean).getReturnType();
	}

	public Object get(Object bean) {
		return resolveGetter(bean).invoke(bean, CommonUtils.EMPTY_OBJECT_ARRAY);
	}

	protected Method resolveGetter(Object bean) {
		return getter;
	}

	protected Method resolveSetter(Object bean) {
		return setter;
	}

	public boolean isReadOnly() {
		return setter == null;
	}

	public void set(Object bean, Object newValue) {
		resolveSetter(bean).invoke(bean, new Object[] { newValue });
	}

	public boolean isWriteOnly() {
		return getter == null;
	}
}
