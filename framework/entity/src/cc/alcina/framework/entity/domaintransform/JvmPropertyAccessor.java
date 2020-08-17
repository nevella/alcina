package cc.alcina.framework.entity.domaintransform;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.PropertyPathAccessor;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.SEUtilities;

public class JvmPropertyAccessor implements PropertyAccessor {
	private Map<String, PropertyPathAccessor> pathAccessors = new LinkedHashMap<String, PropertyPathAccessor>();

	MultikeyMap<Class> typeCache = new UnsortedMultikeyMap<>(2);

	@Override
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

	@Override
	public PropertyReflector getPropertyReflector(Class clazz,
			String propertyName) {
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		return typeCache.ensure(() -> {
			try {
				return SEUtilities
						.getPropertyDescriptorByName(clazz, propertyName)
						.getPropertyType();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}, clazz, propertyName);
	}

	@Override
	public Object getPropertyValue(Object bean, String propertyName) {
		return ensureAccessor(propertyName).getChainedProperty(bean);
	}

	@Override
	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		ensureAccessor(propertyName).setChainedProperty(bean, value);
	}

	private PropertyPathAccessor ensureAccessor(String propertyName) {
		PropertyPathAccessor pathAccessor = pathAccessors.get(propertyName);
		if (pathAccessor == null) {
			pathAccessor = new PropertyPathAccessor(propertyName);
			pathAccessors.put(propertyName, pathAccessor);
		}
		return pathAccessor;
	}

	@Override
	public boolean isReadOnly(Class objectClass, String propertyName) {
		return false;
	}
}
