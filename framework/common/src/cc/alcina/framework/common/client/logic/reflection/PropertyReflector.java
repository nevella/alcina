package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;

public interface PropertyReflector {
	public abstract <A extends Annotation> A
			getAnnotation(Class<A> annotationClass);

	public abstract Class getDefiningType();

	public abstract String getPropertyName();

	public abstract Class getPropertyType();

	public abstract Object getPropertyValue(Object bean);

	public abstract boolean isReadOnly();

	public abstract void setPropertyValue(Object bean, Object newValue);

	default boolean provideWriteableNonTransient() {
		return !isReadOnly()
				&& !getPropertyName().equals("propertyChangeListeners");
	}
}
