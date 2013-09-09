package cc.alcina.framework.entity.domaintransform;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.entity.SEUtilities;

public class JvmPropertyAccessor implements PropertyAccessor {
	public void setPropertyValue(Object bean, String propertyName, Object value) {
		SEUtilities.setPropertyValue(bean, propertyName, value);
	}

	public Object getPropertyValue(Object bean, String propertyName) {
		return SEUtilities.getPropertyValue(bean, propertyName);
	}

	@Override
	public IndividualPropertyAccessor cachedAccessor(Class clazz,
			String propertyName) {
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		try {
			return SEUtilities
					.getPropertyDescriptorByName(targetClass, propertyName)
					.getReadMethod().getAnnotation(annotationClass);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Class getPropertyType(Class clazz, String propertyName) {
		try {
			return SEUtilities.getPropertyDescriptorByName(clazz, propertyName)
					.getPropertyType();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
