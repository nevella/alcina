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
package cc.alcina.framework.entity.transform;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.SelfDescribed;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ImplementationLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.CachingConcurrentMap;
import cc.alcina.framework.entity.util.JvmPropertyReflector;
import cc.alcina.framework.entity.util.MethodWrapper;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;

/**
 * a fair bit of overlap with tltm - should clean up
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class ObjectPersistenceHelper implements ClassLookup, ObjectLookup,
		PropertyAccessor, RegistrableService, BeanDescriptorProvider {
	static volatile ObjectPersistenceHelper singleton;

	public static ObjectPersistenceHelper get() {
		if (singleton == null) {
			singleton = new ObjectPersistenceHelper();
		}
		return singleton;
	}

	public static void register(ObjectPersistenceHelper singleton) {
		ObjectPersistenceHelper.singleton = singleton;
	}

	private ClassLoader servletLayerClassloader;

	private CachingConcurrentMap<String, Class> classNameLookup = new CachingConcurrentMap<String, Class>(
			cn -> {
				if (servletLayerClassloader != null) {
					return servletLayerClassloader.loadClass(cn);
				} else {
					return Class.forName(cn);
				}
			}, 1000);

	private CachingConcurrentMap<Class, String> simpleClassNames = new CachingConcurrentMap<Class, String>(
			Class::getSimpleName, 1000);

	private CachingConcurrentMap<Class, List<PropertyReflector>> classPropertyReflectorLookup = new CachingConcurrentMap<>(
			clazz -> SEUtilities.getPropertyDescriptorsSortedByField(clazz)
					.stream().map(pd -> new JvmPropertyReflector(clazz, pd))
					.collect(Collectors.toList()),
			100);

	private HashMap<Class, BeanDescriptor> cache = new HashMap<Class, BeanDescriptor>();

	// Initialises this. Note - not a thread-specific singleton,
	// any thread (client) specific work delegated to tltm
	public ObjectPersistenceHelper() {
		super();
		init();
	}

	@Override
	public void appShutdown() {
		ThreadlocalTransformManager.get().appShutdown();
	}

	@Override
	public String displayNameForObject(Object o) {
		if (o instanceof HasDisplayName) {
			return ((HasDisplayName) o).displayName();
		}
		return o.toString();
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
			return pd == null ? null
					: pd.getReadMethod() == null ? null
							: pd.getReadMethod().getAnnotation(annotationClass);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public Class getClassForName(String fqn) {
		return classNameLookup.get(fqn);
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
	public List<Class> getInterfaces(Class clazz) {
		return Arrays.asList(clazz.getInterfaces());
	}

	@Override
	public <T extends Entity> T getObject(Class<? extends T> c, long id,
			long localId) {
		// uses thread-local instance
		return TransformManager.get().getObject(c, id, localId);
	}

	@Override
	public <T extends Entity> T getObject(T bean) {
		return (T) TransformManager.get().getObject(bean.entityClass(),
				bean.getId(), bean.getLocalId());
	}

	@Override
	public PropertyReflector getPropertyReflector(Class clazz,
			String propertyName) {
		return (ThreadlocalTransformManager.cast()).getPropertyReflector(clazz,
				propertyName);
	}

	@Override
	public List<PropertyReflector> getPropertyReflectors(Class<?> beanClass) {
		return classPropertyReflectorLookup.get(beanClass);
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		try {
			PropertyDescriptor descriptor = SEUtilities
					.getPropertyDescriptorByName(clazz, propertyName);
			return descriptor == null ? null : descriptor.getPropertyType();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public Object getPropertyValue(Object bean, String propertyName) {
		try {
			PropertyDescriptor descriptor = SEUtilities
					.getPropertyDescriptorByName(bean.getClass(), propertyName);
			if (descriptor == null) {
				throw new Exception(String.format("No property %s for class %s",
						propertyName, bean.getClass().getName()));
			}
			return descriptor.getReadMethod().invoke(bean);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public ClassLoader getServletLayerClassloader() {
		return this.servletLayerClassloader;
	}

	@Override
	public String getSimpleClassName(Class<?> clazz) {
		return simpleClassNames.get(clazz);
	}

	@Override
	public <T> T getTemplateInstance(Class<T> clazz) {
		return newInstance(clazz);
	}

	@Override
	public List<PropertyInfo> getWritableProperties(Class clazz) {
		try {
			List<PropertyInfo> infos = new ArrayList<PropertyInfo>();
			for (PropertyDescriptor pd : SEUtilities
					.getSortedPropertyDescriptors(clazz)) {
				Class<?> propertyType = pd.getPropertyType();
				if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
					continue;
				}
				if (propertyType.isInterface() && propertyType != Set.class
						&& propertyType != List.class
						&& propertyType != Map.class) {
					// this seems to vary (unnecessary on 1.5, necessary on
					// 1.6)-propertydescriptor change probly
					// FIXME - mvcc.jobs.2 - use Alcinapersistenentityimpl if at
					// all
					Class implementation = Registry
							.impl(ImplementationLookup.class)
							.getImplementation(propertyType);
					if (implementation != null) {
						propertyType = implementation;
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
	public boolean isAssignableFrom(Class from, Class to) {
		return from.isAssignableFrom(to);
	}

	@Override
	public boolean isReadOnly(Class objectClass, String propertyName) {
		return (ThreadlocalTransformManager.cast()).isReadOnly(objectClass,
				propertyName);
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
		return (ThreadlocalTransformManager.cast()).newInstance(clazz, objectId,
				localId);
	}

	@Override
	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		if (bean instanceof Entity) {
			(ThreadlocalTransformManager.cast()).setPropertyValue(bean,
					propertyName, value);
		} else {
			SEUtilities.setPropertyValue(bean, propertyName, value);
		}
	}

	public void
			setServletLayerClassloader(ClassLoader servletLayerClassloader) {
		this.servletLayerClassloader = servletLayerClassloader;
	}

	protected Enum getTargetEnumValue(DomainTransformEvent evt) {
		if (evt.getTransformType() == TransformType.NULL_PROPERTY_REF) {
			return null;
		}
		if (Enum.class.isAssignableFrom(evt.getValueClass())) {
			return Enum.valueOf(evt.getValueClass(), evt.getNewStringValue());
		}
		return null;
	}

	protected void init() {
		TransformManager.register(ThreadlocalTransformManager.ttmInstance());
		Reflections.registerClassLookup(this);
		Reflections.registerObjectLookup(this);
		Reflections.registerPropertyAccessor(this);
		Reflections.registerBeanDescriptorProvider(this);
	}
}
