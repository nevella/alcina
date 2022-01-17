package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.Objects;

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

	public boolean provideWriteableNonTransient() {
		return !isReadOnly() && !name.equals("propertyChangeListeners");
	}

	public <A extends Annotation> boolean has(Class<A> annotationClass) {
		return annotationResolver.hasAnnotation(annotationClass);
	}

	public String getName() {
		return this.name;
	}

	public <A extends Annotation> A annotation(Class<A> annotationClass) {
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
		return bean.getClass() == definingType ? getter
				: Reflections.at(bean.getClass()).property(name).getter;
	}

	protected Method resolveSetter(Object bean) {
		return bean.getClass() == definingType ? setter
				: Reflections.at(bean.getClass()).property(name).setter;
	}

	public boolean isReadOnly() {
		return setter == null;
	}

	public boolean isReadable() {
		return getter != null;
	}

	public void set(Object bean, Object newValue) {
		resolveSetter(bean).invoke(bean, new Object[] { newValue });
	}

	public boolean isWriteOnly() {
		return getter == null;
	}

	public boolean isWriteable() {
		return setter != null;
	}

	public void copy(Object from, Object to) {
		set(to, get(from));
	}

	public boolean isDefiningType(Class type) {
		return type == definingType;
	}

	public boolean isPropertyName(String name) {
		return Objects.equals(name, this.name);
	}
}
