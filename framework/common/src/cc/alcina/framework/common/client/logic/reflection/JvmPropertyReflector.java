package cc.alcina.framework.common.client.logic.reflection;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.CommonLocator;

public class JvmPropertyReflector implements PropertyReflector {
	private String propertyName;

	private Class propertyType;

	private Class<?> readMethodDeclaringClass;

	public JvmPropertyReflector(PropertyDescriptor pd) {
		propertyName = pd.getName();
		propertyType = pd.getPropertyType();
		readMethodDeclaringClass = pd.getReadMethod() == null ? null : pd
				.getReadMethod().getDeclaringClass();
	}

	@Override
	public Object getPropertyValue(Object bean) {
		return CommonLocator.get().propertyAccessor()
				.getPropertyValue(bean, getPropertyName());
	}

	@Override
	public void setPropertyValue(Object bean, Object newValue) {
		CommonLocator.get().propertyAccessor()
				.setPropertyValue(bean, getPropertyName(), newValue);
	}

	@Override
	public Class getPropertyType() {
		return propertyType;
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return CommonLocator
				.get()
				.propertyAccessor()
				.getAnnotationForProperty(readMethodDeclaringClass,
						annotationClass, getPropertyName());
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}
}
