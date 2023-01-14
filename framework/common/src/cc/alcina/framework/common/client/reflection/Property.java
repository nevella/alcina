package cc.alcina.framework.common.client.reflection;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.Objects;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class Property implements HasAnnotations {
	private final String name;

	private final Method getter;

	private final Method setter;

	private final Class type;

	private final Class owningType;

	private final AnnotationProvider annotationResolver;

	private final Class declaringType;

	public Property(String name, Method getter, Method setter,
			Class propertyType, Class owningType, Class declaringType,
			AnnotationProvider annotationResolver) {
		this.name = name;
		this.getter = getter;
		this.setter = setter;
		this.type = propertyType;
		this.owningType = owningType;
		this.declaringType = declaringType;
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

	/**
	 * The declarer (location of explicit get/set/is method) (ClassReflector
	 * type) of this property
	 */
	public Class getDeclaringType() {
		return this.declaringType;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * The owner (ClassReflector type) of this property
	 */
	public Class getOwningType() {
		return owningType;
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
		return type == owningType;
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

	public boolean isReadWrite() {
		return getter != null && setter != null;
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

	public boolean provideReadWriteNonTransient() {
		return isReadWrite() && !name.equals("propertyChangeListeners");
	}

	public boolean provideRenderable() {
		return isReadable() && provideNotDefaultIgnoreable();
	}

	public void set(Object bean, Object newValue) {
		resolveSetter(bean).invoke(bean, new Object[] { newValue });
	}

	@Override
	public String toString() {
		return Ax.format("%s.%s : %s", owningType.getSimpleName(), name,
				type.getSimpleName());
	}

	protected Method resolveGetter(Object bean) {
		return bean.getClass() == owningType ? getter
				: Reflections.at(bean).property(name).getter;
	}

	protected Method resolveSetter(Object bean) {
		return bean.getClass() == owningType ? setter
				: Reflections.at(bean).property(name).setter;
	}

	public static class NameComparator implements Comparator<Property> {
		@Override
		public int compare(Property o1, Property o2) {
			return o1.name.compareTo(o2.name);
		}
	}
}
