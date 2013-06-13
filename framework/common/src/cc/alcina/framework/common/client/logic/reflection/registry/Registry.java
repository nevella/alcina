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
package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;

/**
 * 
 * @author Nick Reddel
 */
public class Registry {
	public static final String MARKER_RESOURCE = "registry.properties";

	private ClassLookup classLookup;

	public static Registry get() {
		return instance;
	}

	public void registerBootstrapServices(ClassLookup classLookup) {
		this.classLookup = classLookup;
	}

	public static <V> V impl(Class<V> registryPoint) {
		return get().impl0(registryPoint, void.class, false);
	}

	public static <V> V impl(Class<V> registryPoint, Class targetObjectClass) {
		return get().impl0(registryPoint, targetObjectClass, false);
	}

	public static <V> V impl(Class<V> registryPoint, Class targetObjectClass,
			boolean allowNull) {
		return get().impl0(registryPoint, targetObjectClass, allowNull);
	}

	public static <V> List<V> impls(Class<V> registryPoint) {
		return impls(registryPoint, void.class);
	}

	public static <V> List<V> impls(Class<V> registryPoint, Class targetClass) {
		return get().impls0(registryPoint, targetClass);
	}

	public static <T> T singleton(Class<T> clazz) {
		return get().singleton0(clazz);
	}

	protected HashMap<Class, Map<Class, List<Class>>> registry;

	protected HashMap<Class, Map<Class, Integer>> targetPriority;

	protected LookupMapToMap<Class> exactMap;

	protected LookupMapToMap<ImplementationType> implementationTypeMap;

	protected LookupMapToMap<Object> singletons;

	private static Registry instance = new Registry();

	protected Registry() {
		super();
		registry = new HashMap<Class, Map<Class, List<Class>>>();
		targetPriority = new HashMap<Class, Map<Class, Integer>>();
		singletons = new LookupMapToMap<Object>(2);
		exactMap = new LookupMapToMap<Class>(2);
		implementationTypeMap = new LookupMapToMap<ImplementationType>(2);
	}

	public void appShutdown() {
		instance = null;
	}

	@SuppressWarnings("unchecked")
	public Object instantiateSingle(Class registryPoint, Class targetObject) {
		Class lookupSingle = lookupSingle(registryPoint, targetObject, true);
		return classLookup.newInstance(lookupSingle);
	}

	public Object instantiateSingleOrNull(Class registryPoint,
			Class targetObject) {
		Class lookupSingle = lookupSingle(registryPoint, targetObject, false);
		return lookupSingle != Void.class && lookupSingle != null ? instantiateSingle(
				registryPoint, targetObject) : null;
	}

	public List<Class> lookup(boolean mostSpecificTarget, Class registryPoint,
			Class targetObject, boolean required) {
		// superclasschain
		List<Class> scChain = new ArrayList<Class>();
		Class c = targetObject;
		while (c != null) {
			scChain.add(c);
			c = c.getSuperclass();
		}
		if (!scChain.contains(void.class)) {
			scChain.add(void.class);
		}
		List<Class> result = new ArrayList<Class>();
		Map<Class, List<Class>> map = registry.get(registryPoint);
		if (map == null) {
			if (!required) {
				return new ArrayList<Class>(0);
			}
			throw new RuntimeException(CommonUtils.formatJ(
					"Unable to locate class %s - %s", registryPoint,
					targetObject));
		}
		for (Class sc : scChain) {
			if (map.containsKey(sc)) {
				result.addAll(map.get(sc));
				if (mostSpecificTarget && map.size() != 0) {
					break;
				}
			}
		}
		return CommonUtils.dedupe(result);
	}

	public List<Class> lookup(Class registryPoint) {
		return lookup(false, registryPoint, void.class, true);
	}

	public Class lookupSingle(Class registryPoint, Class targetObject) {
		return lookupSingle(registryPoint, targetObject, false);
	}

	public Class lookupSingle(Class registryPoint, Class targetObject,
			boolean errorOnNull) {
		Class cached = exactMap.get(registryPoint, targetObject);
		if (cached == null) {
			List<Class> lookup = lookup(true, registryPoint, targetObject,
					false);
			cached = lookup.size() > 0 ? lookup.get(0) : Void.class;
			exactMap.put(registryPoint, targetObject, cached);
		}
		if (cached == Void.class && errorOnNull) {
			throw new NoImplementationException(CommonUtils.formatJ(
					"singleton/factory not registered - %s:%s",
					CommonUtils.classSimpleName(registryPoint),
					CommonUtils.classSimpleName(targetObject)));
		}
		return cached;
	}

	public static class NoImplementationException extends RegistryException {
		public NoImplementationException(String message) {
			super(message);
		}
	}

	public void register(Class registeringClass, Class registryPoint) {
		register(registeringClass, registryPoint, void.class,
				ImplementationType.MULTIPLE, 10);
	}

	public static class RegistryException extends RuntimeException {
		public RegistryException() {
			super();
		}

		public RegistryException(String message) {
			super(message);
		}
	}

	public void register(Class registeringClass, Class registryPoint,
			Class targetClass, ImplementationType implementationType,
			int infoPriority) {
		Map<Class, List<Class>> pointMap = registry.get(registryPoint);
		Map<Class, Integer> pointPriority = targetPriority.get(registryPoint);
		if (pointMap == null) {
			pointMap = new HashMap<Class, List<Class>>();
			pointPriority = new HashMap<Class, Integer>();
			registry.put(registryPoint, pointMap);
			targetPriority.put(registryPoint, pointPriority);
		}
		List<Class> registered = pointMap.get(targetClass);
		if (registered == null) {
			registered = new ArrayList<Class>();
			pointMap.put(targetClass, registered);
		}
		if (implementationType == ImplementationType.MULTIPLE
				&& targetClass == void.class
				&& infoPriority != RegistryLocation.DEFAULT_PRIORITY) {
			throw new RegistryException(CommonUtils.formatJ(
					"Non-default priority " + "with Multiple impl type -"
							+ " probably should be instance - %s",
					registeringClass.getName()));
		}
		if (registered.size() == 1
				&& (targetClass != void.class || implementationType != ImplementationType.MULTIPLE)) {
			Integer currentPriority = pointPriority.get(targetClass);
			if (currentPriority > infoPriority) {
				return;
			} else {
				registered.clear();
			}
		}
		registered.add(registeringClass);
		implementationTypeMap.put(registryPoint, targetClass,
				implementationType);
		pointPriority.put(targetClass, infoPriority);
	}

	/*
	 * In some parts (e.g. assignment to implementationTypeMap) we assume only
	 * one (winning) registering class for
	 */
	public void register(Class registeringClass, RegistryLocation info) {
		register(registeringClass, info.registryPoint(), info.targetClass(),
				info.implementationType(), info.priority());
	}

	public static void putSingleton(Class<?> clazz, Object object) {
		get().singletons.put(clazz, void.class, object);
		get().register(object.getClass(), clazz, void.class,
				ImplementationType.SINGLETON, RegistryLocation.MANUAL_PRIORITY);
	}

	public void registerSingleton(Object object, Class<?> clazz) {
		singletons.put(clazz, void.class, object);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class registry:\n");
		for (Class c : registry.keySet()) {
			sb.append(simpleName(c));
			sb.append(": ");
			int x = 0;
			Map<Class, List<Class>> map = registry.get(c);
			for (Class c1 : map.keySet()) {
				if (x++ != 0) {
					sb.append(", ");
				}
				sb.append(simpleName(c1));
				sb.append("={");
				int y = 0;
				for (Class c2 : map.get(c1)) {
					if (y++ != 0) {
						sb.append(", ");
					}
					sb.append(simpleName(c2));
				}
				sb.append("}");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void unregister(Class registeringClass, Class registryPoint) {
		Class tc = void.class;
		registry.get(registryPoint).get(tc).remove(registeringClass);
	}

	private String simpleName(Class c) {
		return c == null ? null : c.getName().contains(".") ? c.getName()
				.substring(c.getName().lastIndexOf(".") + 1) : c.getName();
	}

	protected <V> V impl0(Class<V> registryPoint, Class targetObjectClass,
			boolean allowNull) {
		// optimisation
		Object singleton = singletons.get(registryPoint, targetObjectClass);
		if (singleton != null) {
			if (singleton instanceof RegistryFactory) {
				return (V) ((RegistryFactory) singleton).create(registryPoint,
						targetObjectClass);
			}
			return (V) singleton;
		}
		ImplementationType type = implementationTypeMap.get(registryPoint,
				targetObjectClass);
		Object obj = null;
		if (allowNull) {
			obj = instantiateSingleOrNull(registryPoint, targetObjectClass);
			if (obj == null) {
				return null;
			}
		} else {
			obj = instantiateSingle(registryPoint, targetObjectClass);
		}
		type = type == null ? ImplementationType.MULTIPLE : type;
		switch (type) {
		case FACTORY:
		case SINGLETON:
			singletons.put(registryPoint, targetObjectClass, obj);
			if (type == ImplementationType.FACTORY) {
				return impl0(registryPoint, targetObjectClass, allowNull);
			}
			break;
		case INSTANCE:
		case MULTIPLE:
		}
		return (V) obj;
	}

	protected <V> List<V> impls0(Class<V> registryPoint, Class targetClass) {
		List<Class> impls = get().lookup(false, registryPoint, targetClass,
				false);
		List<V> result = new ArrayList<V>();
		for (Class c : impls) {
			result.add((V) classLookup.newInstance(c));
		}
		return result;
	}

	protected <T> T singleton0(Class<T> clazz) {
		if (clazz == null) {
			return null;
		}
		T impl = (T) singletons.get(clazz, void.class);
		if (impl == null) {
			impl = classLookup.newInstance(clazz);
			registerSingleton(impl, clazz);
		}
		return impl;
	}

	public interface RegistryFactory<V> {
		public V create(Class<V> registryPoint, Class targetObjectClass);
	}
}
