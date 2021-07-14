package cc.alcina.framework.entity.transform;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.entity.util.CachingConcurrentMap;

public class InstantiateOnlyClassLookup implements ClassLookup {
	private ClassLoader reflectiveClassLoader;

	CachingConcurrentMap<String, Class> fqnLookup = new CachingConcurrentMap<>(
			fqn -> {
				try {
					return reflectiveClassLoader.loadClass(fqn);
				} catch (Exception e) {
					throw e;
				}
			}, 50);

	public InstantiateOnlyClassLookup() {
		reflectiveClassLoader = getClass().getClassLoader();
	}

	@Override
	public String displayNameForObject(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class getClassForName(String fqn) {
		return fqnLookup.get(fqn);
	}

	@Override
	public List<PropertyReflector> getPropertyReflectors(Class<?> beanClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T getTemplateInstance(Class<T> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<PropertyInfo> getWritableProperties(Class clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T newInstance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
		throw new UnsupportedOperationException();
	}
}
