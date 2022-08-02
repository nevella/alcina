package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.Objects;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class Property implements HasAnnotations {
	private String name;

	private Method getter;

	private Method setter;

	private Class type;

	private Class definingType;

	private AnnotationProvider annotationResolver;

	public Property(String name, Method getter, Method setter,
			Class propertyType, Class definingType,
			AnnotationProvider annotationResolver) {
		this.name = name;
		this.getter = getter;
		this.setter = setter;
		this.type = propertyType;
		this.definingType = definingType;
		this.annotationResolver = annotationResolver;
	}

	@Override
	public <A extends Annotation> A annotation(Class<A> annotationClass) {
		return annotationResolver.getAnnotation(annotationClass);
	}

	public void copy(Object from, Object to) {
		set(to, get(from));
	}

	public Object get(Object bean) {
		return resolveGetter(bean).invoke(bean, CommonUtils.EMPTY_OBJECT_ARRAY);
	}

	public Class getDefiningType() {
		return definingType;
	}

	public String getName() {
		return this.name;
	}

	public Class getType() {
		return type;
	}

	public Class getType(Object bean) {
		return isWriteOnly() ? getType() : resolveGetter(bean).getReturnType();
	}

	public <A extends Annotation> boolean has(Class<A> annotationClass) {
		return annotationResolver.hasAnnotation(annotationClass);
	}

	public boolean isDefiningType(Class type) {
		return type == definingType;
	}

	public boolean isPropertyName(String name) {
		return Objects.equals(name, this.name);
	}

	public boolean isReadable() {
		return getter != null;
	}

	public boolean isReadOnly() {
		return setter == null;
	}

	public boolean isWriteable() {
		return setter != null;
	}

	public boolean isWriteOnly() {
		return getter == null;
	}

	public boolean provideNotDefaultIgnoreable() {
		switch (name) {
		case "class":
		case "propertyChangeListeners":
			return false;
		default:
			return true;
		}
	}

	public boolean provideWriteableNonTransient() {
		return !isReadOnly() && !name.equals("propertyChangeListeners");
	}

	public void set(Object bean, Object newValue) {
		resolveSetter(bean).invoke(bean, new Object[] { newValue });
	}

	@Override
	public String toString() {
		return Ax.format("%s.%s : %s", definingType.getSimpleName(), name,
				type.getSimpleName());
	}

	protected Method resolveGetter(Object bean) {
		return bean.getClass() == definingType ? getter
				: Reflections.at(bean.getClass()).property(name).getter;
	}

	protected Method resolveSetter(Object bean) {
		return bean.getClass() == definingType ? setter
				: Reflections.at(bean.getClass()).property(name).setter;
	}
}
