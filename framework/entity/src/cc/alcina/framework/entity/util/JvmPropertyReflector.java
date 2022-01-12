package cc.alcina.framework.entity.util;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class JvmPropertyReflector implements Property {
	private static MultikeyMap<JvmPropertyReflector> cache = new UnsortedMultikeyMap<>(
			2);

	public static synchronized JvmPropertyReflector get(Class clazz,
			PropertyDescriptor propertyDescriptor) {
		return cache.ensure(
				() -> new JvmPropertyReflector(Domain.resolveEntityClass(clazz),
						propertyDescriptor),
				Domain.resolveEntityClass(clazz), propertyDescriptor);
	}

	private Class clazz;

	private PropertyDescriptor propertyDescriptor;

	private JvmPropertyReflector(Class clazz,
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
	public Class getDefiningType() {
		return clazz;
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
		return Reflections.property().getPropertyValue(bean,
				getPropertyName());
	}

	@Override
	public boolean isReadOnly() {
		return propertyDescriptor.getWriteMethod() == null;
	}

	@Override
	public void setPropertyValue(Object bean, Object newValue) {
		Reflections.property().setPropertyValue(bean, getPropertyName(),
				newValue);
	}

	@Override
	public String toString() {
		return Ax.format("%s.%s", clazz.getSimpleName(), getPropertyName());
	}
}
