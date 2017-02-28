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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PropertyKeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.MultikeyMapBase.DelegateMapCreator;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap.UnsortedMapCreator;

/**
 *
 * @author Nick Reddel
 */
public class Registry {
	public static final String MARKER_RESOURCE = "registry.properties";

	private static RegistryProvider provider = new BasicRegistryProvider();

	private static DelegateMapCreator delegateCreator = new UnsortedMapCreator();

	public static void appShutdown() {
		provider.appShutdown();
	}

	public static <T> T checkSingleton(Class<T> clazz) {
		return get().singleton0(clazz, true);
	}

	public static void checkSingleton(RegistrySingleton singleton) {
		if (Registry.get().voidPointSingletons
				.containsKey(singleton.getClass().getName())) {
			throw new MultipleSingletonException(singleton.getClass());
		}
	}

	public static <T> T ensureSingleton(Class<T> clazz) {
		return get().singleton0(clazz, false);
	}

	public static Set<RegistryLocation>
			filterForRegistryPointUniqueness(Collection annotations) {
		UnsortedMultikeyMap<RegistryLocation> uniques = new UnsortedMultikeyMap<RegistryLocation>(
				1);
		List<RegistryLocation> locs = new ArrayList<RegistryLocation>();
		for (Object ann : annotations) {
			if (ann instanceof RegistryLocation) {
				locs.add((RegistryLocation) ann);
			} else if (ann instanceof RegistryLocations) {
				locs.addAll(Arrays.asList(((RegistryLocations) ann).value()));
			}
		}
		for (RegistryLocation loc : locs) {
			if (!uniques.containsKey(loc.registryPoint())) {
				uniques.put(loc.registryPoint(), loc);
			} else {
				System.out.println(CommonUtils.formatJ("Discarded - %s, %s",
						CommonUtils.simpleClassName(loc.registryPoint()),
						CommonUtils.simpleClassName(loc.targetClass())));
			}
		}
		return new LinkedHashSet<RegistryLocation>(uniques.allValues());
	}

	public static Set<RegistryLocation> filterForRegistryPointUniqueness(
			Multimap<Class, List<Annotation>> sca) {
		UnsortedMultikeyMap<RegistryLocation> uniques = new UnsortedMultikeyMap<RegistryLocation>(
				2);
		UnsortedMultikeyMap<RegistryLocation> pointsForLastSubclass = new UnsortedMultikeyMap<RegistryLocation>(
				2);
		for (Class clazz : sca.keySet()) {
			List<RegistryLocation> locs = new ArrayList<RegistryLocation>();
			for (Object ann : sca.get(clazz)) {
				if (ann instanceof RegistryLocation) {
					locs.add((RegistryLocation) ann);
				} else if (ann instanceof RegistryLocations) {
					locs.addAll(
							Arrays.asList(((RegistryLocations) ann).value()));
				}
			}
			for (RegistryLocation loc : locs) {
				if (!pointsForLastSubclass.containsKey(loc.registryPoint())) {
					uniques.put(loc.registryPoint(), loc.targetClass(), loc);
				} else {
					if (uniques.get(loc.registryPoint(),
							loc.targetClass()) == loc
							|| loc.registryPoint() == JaxbContextRegistration.class) {
						// inherited, ignore
					} else {
						// System.out
						// .println(CommonUtils.formatJ(
						// "Discarded - %s, %s", CommonUtils
						// .simpleClassName(loc
						// .registryPoint()),
						// CommonUtils.simpleClassName(loc
						// .targetClass())));
					}
				}
			}
			pointsForLastSubclass.putMulti(uniques);
		}
		return new LinkedHashSet<RegistryLocation>(uniques.allValues());
	}

	public static Registry get() {
		return provider.getRegistry();
	}

	public static RegistryProvider getProvider() {
		return provider;
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

	public static <V> V implOrNull(Class<V> registryPoint) {
		return implOrNull(registryPoint, void.class);
	}

	public static <V> V implOrNull(Class<V> registryPoint, Class targetClass) {
		return get().impl0(registryPoint, targetClass, true);
	}

	public static <V> List<V> impls(Class<V> registryPoint) {
		return impls(registryPoint, void.class);
	}

	public static <V> List<V> impls(Class<V> registryPoint, Class targetClass) {
		return get().impls0(registryPoint, targetClass);
	}

	public static <T> Optional<T> optional(Class<T> registryPoint) {
		return Optional.ofNullable(implOrNull(registryPoint));
	}

	public static void registerSingleton(Class<?> registryPoint,
			Object object) {
		get().registerSingleton(registryPoint, void.class, object);
	}

	public static void setDelegateCreator(DelegateMapCreator delegateCreator) {
		Registry.delegateCreator = delegateCreator;
	}

	public static void setProvider(RegistryProvider provider) {
		Registry.provider = provider;
	}

	public static <V> List<V> singletons(Class<V> registryPoint,
			Class targetClass) {
		return get().singletons0(registryPoint, targetClass);
	}

	private ClassLookup classLookup;

	// registrypoint/targetClass/impl/impl
	protected UnsortedMultikeyMap<Class> registry;

	// registrypoint/targetClass/priority
	protected UnsortedMultikeyMap<Integer> targetPriority;

	// registrypoint/targetClass/exact-match-class
	protected UnsortedMultikeyMap<Class> exactMap;

	// registrypoint/targetClass/impl-type
	protected UnsortedMultikeyMap<ImplementationType> implementationTypeMap;

	// registrypoint/targetClass/singleton
	protected UnsortedMultikeyMap<Object> singletons;

	protected Map<String, Object> voidPointSingletons;

	public Registry() {
		registry = new UnsortedMultikeyMap<Class>(3, 0, delegateCreator);
		targetPriority = new UnsortedMultikeyMap<Integer>(2, 0,
				delegateCreator);
		singletons = new UnsortedMultikeyMap<Object>(2, 0, delegateCreator);
		voidPointSingletons = new LinkedHashMap<String, Object>(1000);
		exactMap = new UnsortedMultikeyMap<Class>(2, 0, delegateCreator);
		implementationTypeMap = new UnsortedMultikeyMap<ImplementationType>(2,
				0, delegateCreator);
	}

	public void copyFrom(Registry sourceInstance, Class<?> clazz) {
		registry.asMap(clazz).putMulti(sourceInstance.registry.asMap(clazz));
		targetPriority.asMap(clazz)
				.putMulti(sourceInstance.targetPriority.asMap(clazz));
		exactMap.asMap(clazz).putMulti(sourceInstance.exactMap.asMap(clazz));
		exactMap.asMap(clazz).putMulti(sourceInstance.exactMap.asMap(clazz));
		implementationTypeMap.asMap(clazz)
				.putMulti(sourceInstance.implementationTypeMap.asMap(clazz));
		if (sourceInstance.singletons.containsKey(clazz)) {
			singletons.asMap(clazz)
					.putMulti(sourceInstance.singletons.asMap(clazz));
			String cn = clazz.getName();
			if (voidPointSingletons.containsKey(cn)) {
				voidPointSingletons.put(cn,
						sourceInstance.voidPointSingletons.get(cn));
			}
		}
	}

	public <T> void ensureSingletonRegistered(Class<? super T> clazz, T t) {
		if (impl0(clazz, void.class, true) == null) {
			registerSingleton(clazz, void.class, t);
		}
	}

	public <T> Map<Enum, T> enumLookup(Class<T> registryPoint,
			String propertyName) {
		List<T> handlers = Registry.impls(registryPoint);
		Map<Enum, T> byKey = new LinkedHashMap<>();
		PropertyKeyValueMapper mapper = new PropertyKeyValueMapper(
				propertyName);
		for (T handler : handlers) {
			Enum key = (Enum) mapper.getKey(handler);
			if (byKey.containsKey(key)) {
				throw new RuntimeException(CommonUtils.formatJ(
						"Duplicate key for enum lookup - %s %s %s",
						registryPoint.getClass().getSimpleName(), key,
						handler.getClass().getSimpleName()));
			} else {
				byKey.put(key, handler);
			}
		}
		return byKey;
	}

	public UnsortedMultikeyMap<Class> getRegistry() {
		return this.registry;
	}

	@SuppressWarnings("unchecked")
	public Object instantiateSingle(Class registryPoint, Class targetObject) {
		Class lookupSingle = lookupSingle(registryPoint, targetObject, true);
		return classLookup.newInstance(lookupSingle);
	}

	public Object instantiateSingleOrNull(Class registryPoint,
			Class targetObject) {
		Class lookupSingle = lookupSingle(registryPoint, targetObject, false);
		return lookupSingle != Void.class && lookupSingle != null
				? instantiateSingle(registryPoint, targetObject) : null;
	}

	public List<Class> lookup(boolean mostSpecificTarget, Class registryPoint,
			Class targetObject, boolean required) {
		// superclasschain
		List<Class> scChain = getSuperclassChain(targetObject);
		List<Class> result = new ArrayList<Class>();
		MultikeyMap<Class> pointLookup = registry.asMapEnsure(false,
				registryPoint);
		if (pointLookup == null) {
			if (!required) {
				return new ArrayList<Class>(0);
			}
			throw new RuntimeException(CommonUtils.formatJ(
					"Unable to locate %s - %s", registryPoint, targetObject));
		}
		for (Class sc : scChain) {
			if (pointLookup.containsKey(sc)) {
				result.addAll((Collection) pointLookup.keys(sc));
				if (mostSpecificTarget && pointLookup.size() != 0) {
					break;
				}
			}
		}
		return CommonUtils.dedupe(result);
	}

	public List<Class> lookup(Class registryPoint) {
		return lookup(false, registryPoint, void.class, true);
	}

	public <T> T lookupImplementation(Class<T> registryPoint, Enum value,
			String propertyName) {
		return lookupImplementation(registryPoint, value, propertyName, false);
	}

	public <T> T lookupImplementation(Class<T> registryPoint, Enum value,
			String propertyName, boolean newInstance) {
		Map<Enum, T> byKey = enumLookup(registryPoint, propertyName);
		T t = byKey.get(value);
		if (t != null && newInstance) {
			try {
				t = (T) classLookup.newInstance(t.getClass());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return t;
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
			synchronized (exactMap) {
				exactMap.put(registryPoint, targetObject, cached);
			}
		}
		if (cached == Void.class && errorOnNull) {
			throw new NoImplementationException(CommonUtils.formatJ(
					"singleton/factory not registered - %s:%s",
					CommonUtils.classSimpleName(registryPoint),
					CommonUtils.classSimpleName(targetObject)));
		}
		return cached;
	}

	public void register(Class registeringClass, Class registryPoint) {
		register(registeringClass, registryPoint, void.class,
				ImplementationType.MULTIPLE, 10);
	}

	public synchronized void register(Class registeringClass,
			Class registryPoint, Class targetClass,
			ImplementationType implementationType, int infoPriority) {
		MultikeyMap<Class> registered = registry.asMapEnsure(true,
				registryPoint, targetClass);
		UnsortedMultikeyMap<Class> pointMap = null;
		if (implementationType == ImplementationType.MULTIPLE
				&& targetClass == void.class
				&& infoPriority != RegistryLocation.DEFAULT_PRIORITY) {
			throw new RegistryException(CommonUtils.formatJ(
					"Non-default priority " + "with Multiple impl type -"
							+ " probably should be instance - %s",
					registeringClass.getName()));
		}
		if (registered.size() == 1 && (targetClass != void.class
				|| implementationType != ImplementationType.MULTIPLE)) {
			Integer currentPriority = targetPriority.get(registryPoint,
					targetClass);
			if (currentPriority > infoPriority) {
				return;
			} else {
				registered.clear();
			}
		}
		registered.put(registeringClass, registeringClass);
		implementationTypeMap.put(registryPoint, targetClass,
				implementationType);
		targetPriority.put(registryPoint, targetClass, infoPriority);
	}

	/*
	 * In some parts (e.g. assignment to implementationTypeMap) we assume only
	 * one (winning) registering class for
	 */
	public void register(Class registeringClass, RegistryLocation info) {
		register(registeringClass, info.registryPoint(), info.targetClass(),
				info.implementationType(), info.priority());
	}

	public void registerBootstrapServices(ClassLookup classLookup) {
		this.classLookup = classLookup;
	}

	public void registerSingleton(Class<?> registryPoint, Class<?> targetClass,
			Object object) {
		registerSingletonInLookups(registryPoint, targetClass, object);
		register(object.getClass(), registryPoint, targetClass,
				ImplementationType.SINGLETON, RegistryLocation.MANUAL_PRIORITY);
	}

	public void shareSingletonMapTo(Registry otherRegistry) {
		otherRegistry.singletons = singletons;
		otherRegistry.voidPointSingletons = voidPointSingletons;
	}

	public void shutdownSingletons() {
		for (Object o : singletons.allValues()) {
			if (o instanceof RegistrableService) {
				try {
					((RegistrableService) o).appShutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		singletons.clear();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Class registry:\n");
		for (Class c : ((Map<Class, ?>) registry).keySet()) {
			sb.append(simpleName(c));
			sb.append(": ");
			int x = 0;
			Map<Class, Map<Class, Class>> map = registry.asMap(c).delegate();
			for (Class c1 : map.keySet()) {
				if (x++ != 0) {
					sb.append(", ");
				}
				sb.append(simpleName(c1));
				sb.append("={");
				int y = 0;
				for (Class c2 : map.get(c1).keySet()) {
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

	public void unregister(Class registryPoint, Class targetClass,
			Class registeringClass) {
		registry.remove(registryPoint, targetClass, registeringClass);
	}

	private synchronized void registerSingletonInLookups(Class<?> registryPoint,
			Class<?> targetClass, Object object) {
		boolean voidTarget = targetClass == void.class;
		singletons.put(registryPoint, targetClass, object);
		if (voidTarget) {
			// use className so we don't have to get class objects from
			// different parts of memory - this did seem to help a jvm
			// optimisation
			voidPointSingletons.put(registryPoint.getName(), object);
		}
	}

	private String simpleName(Class c) {
		return c == null ? null
				: c.getName().contains(".") ? c.getName().substring(
						c.getName().lastIndexOf(".") + 1) : c.getName();
	}

	protected List<Class> getSuperclassChain(Class targetObject) {
		List<Class> scChain = new ArrayList<Class>();
		Class c = targetObject;
		while (c != null) {
			scChain.add(c);
			c = c.getSuperclass();
		}
		if (!scChain.contains(void.class)) {
			scChain.add(void.class);
		}
		return scChain;
	}

	protected <V> V impl0(Class<V> registryPoint, Class targetObjectClass,
			boolean allowNull) {
		// optimisation
		Object singleton = null;
		boolean voidTarget = targetObjectClass == void.class;
		if (voidTarget) {
			singleton = voidPointSingletons.get(registryPoint.getName());
		} else {
			singleton = singletons.get(registryPoint, targetObjectClass);
		}
		if (singleton != null && !(singleton instanceof RegistryFactory)) {
			return (V) singleton;
		}
		ImplementationType type = resolveImplementationType(registryPoint,
				targetObjectClass, allowNull);
		Object obj = null;
		if (singleton == null) {
			if (allowNull) {
				obj = instantiateSingleOrNull(registryPoint, targetObjectClass);
				if (obj == null) {
					return null;
				}
			} else {
				obj = instantiateSingle(registryPoint, targetObjectClass);
			}
		}
		type = type == null ? ImplementationType.MULTIPLE : type;
		switch (type) {
		case FACTORY:
			if (singleton == null) {
				registerSingletonInLookups(registryPoint, targetObjectClass,
						obj);
				singleton = obj;
			}
			return (V) ((RegistryFactory) singleton).create(registryPoint,
					targetObjectClass);
		case SINGLETON:
			if (singleton == null) {
				registerSingletonInLookups(registryPoint, targetObjectClass,
						obj);
				singleton = obj;
			}
			return (V) singleton;
		case INSTANCE:
		case MULTIPLE:
		}
		return (V) obj;
	}

	protected <V> List<V> impls0(Class<V> registryPoint, Class targetClass) {
		List<Class> impls = lookup(false, registryPoint, targetClass, false);
		List<V> result = new ArrayList<V>();
		for (Class c : impls) {
			result.add((V) classLookup.newInstance(c));
		}
		return result;
	}

	protected <V> ImplementationType resolveImplementationType(
			Class<V> registryPoint, Class targetObjectClass,
			boolean allowNull) {
		ImplementationType type = implementationTypeMap.get(registryPoint,
				targetObjectClass);
		if (type != null) {
			return type;
		}
		List<Class> scChain = getSuperclassChain(targetObjectClass);
		for (Class sc : scChain) {
			type = implementationTypeMap.get(registryPoint, sc);
			if (type != null) {
				implementationTypeMap.put(registryPoint, targetObjectClass,
						type);
				return type;
			}
		}
		if (allowNull) {
			return null;
		}
		throw new RuntimeException(CommonUtils.formatJ(
				"Registry: no resolved implementation type for %s :: %s",
				CommonUtils.simpleClassName(registryPoint),
				CommonUtils.simpleClassName(targetObjectClass)));
	}

	protected <T> T singleton0(Class<T> clazz,
			boolean returnNullIfNotRegistered) {
		if (clazz == null) {
			return null;
		}
		T impl = (T) voidPointSingletons.get(clazz.getName());
		if (impl == null && !returnNullIfNotRegistered) {
			impl = classLookup.newInstance(clazz);
			registerSingleton(clazz, impl);
		}
		return impl;
	}

	protected <V> List<V> singletons0(Class<V> registryPoint,
			Class targetClass) {
		List<Class> impls = get().lookup(false, registryPoint, targetClass,
				false);
		List<V> result = new ArrayList<V>();
		for (Class c : impls) {
			result.add((V) ensureSingleton(c));
		}
		return result;
	}

	@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
	public static class BasicRegistryProvider implements RegistryProvider {
		private volatile Registry instance;

		@Override
		public void appShutdown() {
			getRegistry().shutdownSingletons();
			Registry.setProvider(null);
		}

		@Override
		public Registry getRegistry() {
			if (instance == null) {
				synchronized (this) {
					instance = new Registry();
				}
			}
			return instance;
		}
	}

	public static class MultipleSingletonException extends RuntimeException {
		public MultipleSingletonException(Class<?> clazz) {
			super(CommonUtils.formatJ(
					"Constructor of singleton %s invoked more than once",
					clazz.getName()));
		}
	}

	public static class NoImplementationException extends RegistryException {
		public NoImplementationException(String message) {
			super(message);
		}
	}

	public static class RegistryException extends RuntimeException {
		public RegistryException() {
			super();
		}

		public RegistryException(String message) {
			super(message);
		}
	}

	public interface RegistryFactory<V> {
		public V create(Class<? extends V> registryPoint,
				Class targetObjectClass);
	}

	public static interface RegistryProvider {
		void appShutdown();

		Registry getRegistry();
	}
}
