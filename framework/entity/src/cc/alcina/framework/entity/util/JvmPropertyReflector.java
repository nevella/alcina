package cc.alcina.framework.entity.util;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;

public class JvmPropertyReflector implements PropertyReflector {
	private Class clazz;

	private PropertyDescriptor propertyDescriptor;

	public JvmPropertyReflector(Class clazz,
			PropertyDescriptor propertyDescriptor) {
		this.clazz = clazz;
		this.propertyDescriptor = propertyDescriptor;
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return propertyDescriptor.getReadMethod() == null ? null
				: propertyDescriptor.getReadMethod()
						.getAnnotation(annotationClass);
	}

	@Override
	public String getPropertyName() {
		return propertyDescriptor.getName();
	}

	@Override
	public Class getPropertyType() {
		return propertyDescriptor.getPropertyType();
	}

	@Override
	public Object getPropertyValue(Object bean) {
		return Reflections.propertyAccessor().getPropertyValue(bean,
				getPropertyName());
	}

	@Override
	public void setPropertyValue(Object bean, Object newValue) {
		Reflections.propertyAccessor().setPropertyValue(bean, getPropertyName(),
				newValue);
	}

	@Override
	public String toString() {
		return Ax.format("%s.%s", clazz.getSimpleName(), getPropertyName());
	}

	@Override
	public Class getDefiningType() {
		return clazz;
	}

	@Override
	public boolean isReadOnly() {
		return propertyDescriptor.getWriteMethod() == null;
	}
}
