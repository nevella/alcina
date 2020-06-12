package cc.alcina.framework.entity.util;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;

public class JvmPropertyReflector implements PropertyReflector {
	private String propertyName;

	private Class propertyType;

	private Class<?> readMethodDeclaringClass;

	public JvmPropertyReflector(PropertyDescriptor pd) {
		propertyName = pd.getName();
		propertyType = pd.getPropertyType();
		readMethodDeclaringClass = pd.getReadMethod() == null ? null
				: pd.getReadMethod().getDeclaringClass();
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return readMethodDeclaringClass == null ? null
				: SEUtilities
						.getPropertyDescriptorByName(readMethodDeclaringClass,
								getPropertyName())
						.getReadMethod().getAnnotation(annotationClass);
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Class getPropertyType() {
		return propertyType;
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
		return Ax.format("%s.%s", readMethodDeclaringClass.getSimpleName(),
				propertyName);
	}
}
