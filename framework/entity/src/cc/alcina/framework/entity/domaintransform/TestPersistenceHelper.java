/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.domaintransform;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.SelfDescribed;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ImplementationLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.MethodWrapper;
import cc.alcina.framework.gwt.client.gwittir.HasGeneratedDisplayName;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;

/**
 * j2se, but no ref to tltm
 * 
 * @author nick@alcina.cc
 * 
 */
public class TestPersistenceHelper implements ClassLookup, ObjectLookup,
		PropertyAccessor, BeanDescriptorProvider {
	public static TestPersistenceHelper get() {
		TestPersistenceHelper singleton = Registry
				.checkSingleton(TestPersistenceHelper.class);
		if (singleton == null) {
			singleton = new TestPersistenceHelper();
			Registry.registerSingleton(TestPersistenceHelper.class, singleton);
		}
		return singleton;
	}

	private ClassLoader reflectiveClassLoader;

	CachingMap<String, Class> fqnLookup = new CachingMap<String, Class>(fqn -> {
		try {
			return reflectiveClassLoader.loadClass(fqn);
		} catch (Exception e) {
			throw e;
		}
	});

	private HashMap<Class, BeanDescriptor> cache = new HashMap<Class, BeanDescriptor>();

	private TestPersistenceHelper() {
		super();
		reflectiveClassLoader = getClass().getClassLoader();
		Reflections.registerClassLookup(this);
		Reflections.registerObjectLookup(this);
		Reflections.registerPropertyAccessor(this);
		Reflections.registerBeanDescriptorProvider(this);
	}

	@Override
	public String displayNameForObject(Object o) {
		if (o instanceof HasGeneratedDisplayName) {
			return ((HasGeneratedDisplayName) o).generatedDisplayName();
		}
		String dnpn = "id";
		Bean info = o.getClass().getAnnotation(Bean.class);
		if (info != null) {
			dnpn = info.displayNamePropertyName();
		}
		Object pv = Reflections.propertyAccessor().getPropertyValue(o, dnpn);
		return (pv == null) ? "---" : pv.toString();
	}

	public List<String> getAnnotatedPropertyNames(Class clazz) {
		try {
			List<String> result = new ArrayList<String>();
			PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null && pd.getWriteMethod() != null
						&& pd.getReadMethod()
								.getAnnotation(Display.class) != null) {
					result.add(pd.getName());
				}
			}
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass) {
		return (A) targetClass.getAnnotation(annotationClass);
	}

	@Override
	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		try {
			PropertyDescriptor pd = SEUtilities
					.getPropertyDescriptorByName(targetClass, propertyName);
			if (pd != null) {
				return pd.getReadMethod().getAnnotation(annotationClass);
			}
			return null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public Class getClassForName(String fqn) {
		return fqnLookup.get(fqn);
	}

	@Override
	public BeanDescriptor getDescriptor(Object object) {
		if (cache.containsKey(object.getClass())) {
			return cache.get(object.getClass());
		}
		BeanDescriptor result = null;
		if (object instanceof SelfDescribed) {
			// System.out.println("SelfDescribed\t"+
			// object.getClass().getName());
			result = ((SelfDescribed) object).__descriptor();
		} else {
			// System.out.println("Reflection\t"+ object.getClass().getName());
			result = new ReflectionBeanDescriptor(object.getClass());
			cache.put(object.getClass(), result);
		}
		return result;
	}

	@Override
	public <T extends Entity> T getObject(Class<? extends T> c, long id,
			long localId) {
		// uses thread-local instance
		return TransformManager.get().getObject(c, id, localId);
	}

	@Override
	public <T extends Entity> T getObject(T bean) {
		return (T) TransformManager.get().getObject(bean.getClass(),
				bean.getId(), bean.getLocalId());
	}

	@Override
	public List<PropertyReflector> getPropertyReflectors(Class<?> beanClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals(propertyName)) {
					return pd.getPropertyType();
				}
			}
			return null;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public Object getPropertyValue(Object bean, String propertyName) {
		return SEUtilities.getPropertyValue(bean, propertyName);
	}

	public ClassLoader getReflectiveClassLoader() {
		return this.reflectiveClassLoader;
	}

	@Override
	public <T> T getTemplateInstance(Class<T> clazz) {
		return newInstance(clazz);
	}

	@Override
	public List<PropertyInfo> getWritableProperties(Class clazz) {
		try {
			List<PropertyInfo> infos = new ArrayList<PropertyInfo>();
			java.beans.BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				Class<?> propertyType = pd.getPropertyType();
				if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
					continue;
				}
				if (propertyType.isInterface() && propertyType != Set.class
						&& propertyType != List.class
						&& propertyType != Map.class) {
					// this seems to vary (unnecessary on 1.5, necessary on
					// 1.6)-propertydescriptor change probly
					Class<?> implementationType = Registry
							.impl(ImplementationLookup.class)
							.getImplementation(propertyType);
					if (implementationType != null) {
						propertyType = implementationType;
					}
				}
				infos.add(new PropertyInfo(propertyType, pd.getName(),
						new MethodWrapper(pd.getReadMethod()),
						new MethodWrapper(pd.getWriteMethod()), clazz));
			}
			return infos;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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
		try {
			Entity newInstance = (Entity) clazz.newInstance();
			newInstance.setLocalId(localId);
			return (T) newInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public PropertyReflector property(Class clazz, String propertyName) {
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	@Override
	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		SEUtilities.setPropertyValue(bean, propertyName, value);
	}

	public void setReflectiveClassLoader(ClassLoader reflectiveClassLoader) {
		this.reflectiveClassLoader = reflectiveClassLoader;
	}

	protected Enum getTargetEnumValue(DomainTransformEvent evt) {
		if (Enum.class.isAssignableFrom(evt.getValueClass())) {
			return Enum.valueOf(evt.getValueClass(), evt.getNewStringValue());
		}
		return null;
	}
}
