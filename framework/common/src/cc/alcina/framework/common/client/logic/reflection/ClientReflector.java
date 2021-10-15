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
package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

/**
 * 
 * @author Nick Reddel
 */
@ReflectionModule("Initial")
public abstract class ClientReflector implements ClassLookup {
	private static ClientReflector domainReflector;

	/**
	 * Because ClientReflector is used from server code as well (via
	 * TransformManager), we don't use a public static final INSTANCE generated
	 * instance (see defined())
	 * <p>
	 * -- correction to this, it's minimally reffed - and that should probably
	 * go
	 * </p>
	 * 
	 */
	public static ClientReflector get() {
		if (domainReflector == null) {
			domainReflector = ClientReflectorFactory.create();
		}
		return domainReflector;
	}

	public static void register(ClientReflector r) {
		domainReflector = r;
	}

	protected Map<Class, ClientBeanReflector> gwbiMap;

	private List<ClientReflector> reflectors = new ArrayList<ClientReflector>();

	Set<Class> nullReflectors = new LinkedHashSet<>();

	private Map<Class, Object> templateInstances;

	protected Map<String, Class> forNameMap;

	protected Map<String, List<String>> clazzImplements;

	private MultikeyMap<Boolean> assignableFrom = new UnsortedMultikeyMap<>(2);

	private Map<Class, List<Class>> perClassInterfaces;

	CachingMap<Class<?>, Map<String, PropertyReflector>> propertyReflectorsCache = new CachingMap<>(
			beanClass -> ClientReflector.get().beanInfoForClass(beanClass)
					.getPropertyReflectors().values().stream()
					.collect(AlcinaCollectors
							.toKeyMap(PropertyReflector::getPropertyName)));

	public ClientReflector() {
		gwbiMap = createClassKeyMap();
		templateInstances = createClassKeyMap();
		perClassInterfaces = createClassKeyMap();
		forNameMap = createStringKeyMap();
		clazzImplements = createStringKeyMap();
		{
			CommonUtils.stdAndPrimitivesMap.forEach(forNameMap::put);
		}
		reflectors.add(this);
	}

	public List<String> allInterestingAlcinaBeanProperties(Object bean) {
		return getWritableProperties(bean.getClass()).stream()
				.map(p -> p.getPropertyName()).sorted()
				.collect(Collectors.toList());
	}

	public ClientBeanReflector beanInfoForClass(Class clazz) {
		clazz = getBeanInfoClassResolver().resolveForBeanInfo(clazz);
		ClientBeanReflector beanReflector = gwbiMap.get(clazz);
		if (beanReflector == null) {
			if (!gwbiMap.containsKey(clazz)) {
				for (ClientReflector reflector : reflectors) {
					reflector.initReflector(clazz);
				}
				beanReflector = gwbiMap.get(clazz);
				if (beanReflector == null) {
					// so we don't look again (until next module registered)
					gwbiMap.put(clazz, null);
					nullReflectors.add(clazz);
				}
			}
		}
		return beanReflector;
	}

	@Override
	public String displayNameForObject(Object o) {
		ClientBeanReflector bi = beanInfoForClass(o.getClass());
		return (bi == null) ? null : bi.getObjectName(o);
	}

	@Override
	public <A extends Annotation> A getAnnotationForClass(Class targetClass,
			Class<A> annotationClass) {
		ClientBeanReflector beanInfo = ClientReflector.get()
				.beanInfoForClass(targetClass);
		return beanInfo == null ? null
				: beanInfo.getAnnotation(annotationClass);
	}

	@Override
	public Class getClassForName(String fqn) {
		Class clazz = forNameMap.get(fqn);
		if (clazz != null) {
			return clazz;
		}
		throw new WrappedRuntimeException(
				Ax.format("Class %s not reflect-instantiable", fqn),
				SuggestedAction.NOTIFY_ERROR);
	}

	@Override
	public List<Class> getInterfaces(Class clazz) {
		return perClassInterfaces.computeIfAbsent(clazz, k -> {
			List<String> list = clazzImplements.get(k.getName());
			return list == null ? Collections.emptyList()
					: list.stream().map(Reflections::forName)
							.collect(Collectors.toList());
		});
	}

	@Override
	public Map<String, PropertyReflector>
			getPropertyReflectors(Class<?> beanClass) {
		return propertyReflectorsCache.get(beanClass);
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		Property property = GwittirBridge.get().getPropertyForClass(clazz,
				propertyName);
		if (property == null) {
			throw new NoSuchPropertyException(
					Ax.format("%s.%s", clazz.getSimpleName(), propertyName));
		}
		return property.getType();
	}

	@Override
	public <T> T getTemplateInstance(Class<T> clazz) {
		clazz = getBeanInfoClassResolver().resolveForBeanInfo(clazz);
		if (!templateInstances.containsKey(clazz)) {
			templateInstances.put(clazz,
					Reflections.classLookup().newInstance(clazz, 0, 0));
		}
		return (T) templateInstances.get(clazz);
	}

	@Override
	public List<PropertyInfo> getWritableProperties(Class clazz) {
		List<PropertyInfo> infos = new ArrayList<PropertyInfo>();
		BeanDescriptor descriptor = GwittirBridge.get()
				.getDescriptorForClass(clazz);
		if (descriptor == null) {
			return infos;
		}
		for (Property p : descriptor.getProperties()) {
			if (p.getMutatorMethod() == null) {
				continue;
			}
			infos.add(new PropertyInfo(p.getType(), p.getName(),
					p.getAccessorMethod(), p.getMutatorMethod(), clazz));
		}
		return infos;
	}

	@Override
	public boolean handlesClass(Class clazz) {
		return beanInfoForClass(clazz) != null;
	}

	@Override
	public boolean isAssignableFrom(Class from, Class to) {
		return assignableFrom.ensure(() -> {
			Set<String> checked = new HashSet<>();
			Set<String> stack = new HashSet<>();
			String fromName = from.getName();
			Class cursor = to;
			while (cursor != Object.class && cursor != null) {
				if (from == cursor) {
					return true;
				}
				List<String> interfaces = clazzImplements.get(cursor.getName());
				if (interfaces != null) {
					stack.addAll(interfaces);
				}
				cursor = cursor.getSuperclass();
			}
			while (stack.size() > 0) {
				Iterator<String> iterator = stack.iterator();
				String type = iterator.next();
				iterator.remove();
				if (fromName.equals(type)) {
					return true;
				}
				checked.add(type);
				List<String> interfaces = clazzImplements.get(type);
				if (interfaces != null) {
					interfaces.stream().filter(t -> !checked.contains(t))
							.forEach(stack::add);
				}
			}
			return false;
		}, from, to);
	}

	public boolean isInstantiableClass(Class clazz) {
		try {
			if (CommonUtils.stdAndPrimitives.contains(clazz)) {
				return false;
			}
			getClassForName(clazz.getName());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public <T> T newInstance(Class<T> clazz) {
		return newInstance(clazz, 0, 0);
	}

	@Override
	public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
		for (ClientReflector reflector : reflectors) {
			T instance = reflector.newInstance0(clazz, objectId, localId);
			if (instance != null) {
				if (instance instanceof Entity) {
					((Entity) instance).setLocalId(localId);
				}
				return instance;
			}
		}
		for (ClientReflector reflector : reflectors) {
			reflector.initialiseNewInstance(clazz);
			T instance = reflector.newInstance0(clazz, objectId, localId);
			if (instance != null) {
				if (instance instanceof Entity) {
					((Entity) instance).setLocalId(localId);
				}
				return instance;
			}
		}
		throw new RuntimeException(
				"Class " + clazz + " not reflect-instantiable");
	}

	public void registerChild(ClientReflector child) {
		reflectors.add(0, child);
		child.gwbiMap = gwbiMap;
		forNameMap.putAll(child.forNameMap);
		child.clazzImplements.forEach((from, to) -> {
			List<String> list = clazzImplements.get(from);
			if (list != null) {
				clazzImplements.put(from,
						Stream.concat(list.stream(), to.stream())
								.collect(Collectors.toList()));
			} else {
				clazzImplements.put(from, to);
			}
		});
		gwbiMap.keySet().removeAll(nullReflectors);
		nullReflectors.clear();
	}

	private BeaninfoClassResolver getBeanInfoClassResolver() {
		return Registry.impl(BeaninfoClassResolver.class);
	}

	protected Map createClassKeyMap() {
		// FIXME - 2022 - hide this behind delegation
		return GWT.isClient() ? JsUniqueMap.create(Class.class, true)
				: new LinkedHashMap<>();
	}

	protected Map createStringKeyMap() {
		return GWT.isClient() ? JsUniqueMap.create(String.class, true)
				: new LinkedHashMap<>();
	}

	protected abstract void initialiseNewInstance(Class clazz);

	protected void initReflector(Class clazz) {
	}

	protected abstract <T> T newInstance0(Class<T> clazz, long objectId,
			long localId);

	@RegistryLocation(registryPoint = BeaninfoClassResolver.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class BeaninfoClassResolver {
		public Class resolveForBeanInfo(Class clazz) {
			return clazz;
		}
	}
}
