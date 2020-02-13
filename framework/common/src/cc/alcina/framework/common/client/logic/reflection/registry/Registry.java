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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PropertyKeyValueMapper;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DelegateMapCreator;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap.UnsortedMapCreator;

/**
 *
 * @author Nick Reddel
 * 
 *         <h3>To add jvm-only functionality</h3>
 *         <p>
 *         This is equivalent to "add a class in a location visible to both the
 *         bytecode and gwt compilers where the default implementation is a
 *         noop".
 * 
 *         Since we know the default implementation, make the noop
 *         implementation be the registrypoint itself:
 *         </p>
 * 
 *         <pre>
 *         // ImplementationType.INSTANCE required for registry override
 *         &#64;RegistryLocation(registryPoint = DoSomethingFunky.class, implementationType = ImplementationType.INSTANCE)
 *         public class DoSomethingFunky {
 *         	public void justDoIt() {
 *         		// noop
 *         	}
 *         }
 *         </pre>
 *         <p>
 *         and, visible to the bytecode compiler only:
 *         </p>
 * 
 *         <pre>
 *         // priority=RegistryLocation.MANUAL_PRIORITY means use this by preference
 *         // (when visible)
 *         &#64;RegistryLocation(registryPoint = DoSomethingFunky.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.MANUAL_PRIORITY)
 *         public class DoSomethingFunkyJvmImpl {
 *         	public void justDoIt() {
 *         		// do incredible jvm-only deeds
 *         	}
 *         }
 *         </pre>
 * 
 * 
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
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
				// System.out.println(Ax.format("Discarded - %s, %s",
				// CommonUtils.simpleClassName(loc.registryPoint()),
				// CommonUtils.simpleClassName(loc.targetClass())));
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
						// .println(Ax.format(
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

	public static boolean hasImpl(Class<?> clazz) {
		return Registry.implOrNull(clazz) != null;
	}

	public static <V> V impl(Class<V> registryPoint) {
		return get().impl0(registryPoint, void.class, false);
	}

	public static <V> V impl(Class<V> registryPoint, Class targetClass) {
		return get().impl0(registryPoint, targetClass, false);
	}

	public static <V> V impl(Class<V> registryPoint, Class targetClass,
			boolean allowNull) {
		return get().impl0(registryPoint, targetClass, allowNull);
	}

	public static <V> Optional<V> implOptional(Class<V> registryPoint) {
		return Optional.<V> ofNullable(implOrNull(registryPoint, void.class));
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

	public static void registerSingleton(Class<?> registryPoint, Object object,
			boolean replaceExisting) {
		get().registerSingleton(registryPoint, void.class, object,
				replaceExisting);
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

	private String name;

	private RegistryKeys keys;

	private ClassLookup classLookup;

	// registrypoint/targetClass/impl/impl
	protected UnsortedMultikeyMap<RegistryKey> registry;

	// registrypoint/targetClass/priority
	protected UnsortedMultikeyMap<Integer> targetPriority;

	// registrypoint/targetClass/exact-match-class
	protected UnsortedMultikeyMap<RegistryKey> exactMap;

	// registrypoint/targetClass/impl-type
	protected UnsortedMultikeyMap<ImplementationType> implementationTypeMap;

	// registrypoint/targetClass/singleton
	protected UnsortedMultikeyMap<Object> singletons;

	protected Map<String, Object> voidPointSingletons;

	private Map<Class, Object> implClassesRegistered;

	public Registry() {
		keys = new RegistryKeys();
		registry = new UnsortedMultikeyMap<RegistryKey>(3, 0, delegateCreator);
		targetPriority = new UnsortedMultikeyMap<Integer>(2, 0,
				delegateCreator);
		singletons = new UnsortedMultikeyMap<Object>(2, 0, delegateCreator);
		voidPointSingletons = new LinkedHashMap<String, Object>(1000);
		exactMap = new UnsortedMultikeyMap<RegistryKey>(2, 0, delegateCreator);
		implementationTypeMap = new UnsortedMultikeyMap<ImplementationType>(2,
				0, delegateCreator);
		implClassesRegistered = new LinkedHashMap<>();
	}

	public void copyFrom(Registry sourceInstance, Class<?> clazz) {
		RegistryKey key = keys.get(clazz);
		registry.asMap(key).putMulti(sourceInstance.registry.asMap(key));
		targetPriority.asMap(key)
				.putMulti(sourceInstance.targetPriority.asMap(key));
		exactMap.asMap(key).putMulti(sourceInstance.exactMap.asMap(key));
		exactMap.asMap(key).putMulti(sourceInstance.exactMap.asMap(key));
		implementationTypeMap.asMap(key)
				.putMulti(sourceInstance.implementationTypeMap.asMap(key));
		if (sourceInstance.singletons.containsKey(key)) {
			singletons.asMap(key)
					.putMulti(sourceInstance.singletons.asMap(key));
			String cn = key.name();
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
				throw new RuntimeException(Ax.format(
						"Duplicate key for enum lookup - %s %s %s",
						registryPoint.getClass().getSimpleName(), key,
						handler.getClass().getSimpleName()));
			} else {
				byKey.put(key, handler);
			}
		}
		return byKey;
	}

	public UnsortedMultikeyMap<RegistryKey> getRegistry() {
		return this.registry;
	}

	@SuppressWarnings("unchecked")
	public Object instantiateSingle(Class registryPoint, Class targetClass) {
		Class lookupSingle = lookupSingle(registryPoint, targetClass, true);
		return classLookup.newInstance(lookupSingle);
	}

	public Object instantiateSingleOrNull(Class registryPoint,
			Class targetClass) {
		Class lookupSingle = lookupSingle(registryPoint, targetClass, false);
		return lookupSingle != Void.class && lookupSingle != null
				? instantiateSingle(registryPoint, targetClass)
				: null;
	}

	public RegistryKey key(String className) {
		return keys.get(className);
	}

	public List<Class> lookup(boolean mostSpecificTarget, Class registryPoint,
			Class targetClass, boolean required) {
		RegistryKey registryPointKey = keys.get(registryPoint);
		// superclasschain
		List<Class> scChain = getSuperclassChain(targetClass);
		Set<RegistryKey> matched = new LinkedHashSet<>();
		MultikeyMap<RegistryKey> pointLookup = registry.asMapEnsure(false,
				registryPointKey);
		if (pointLookup == null) {
			if (!required) {
				return new ArrayList<>(0);
			}
			System.out.println(registry.toString());
			throw new RuntimeException(Ax.format(
					"Unable to locate %s - %s", registryPoint, targetClass));
		}
		for (Class sc : scChain) {
			RegistryKey pointKey = keys.get(sc);
			Collection<RegistryKey> targetKeys = pointLookup.keys(pointKey);
			if (targetKeys != null) {
				matched.addAll(targetKeys);
				if (mostSpecificTarget && pointLookup.size() != 0) {
					break;
				}
			}
		}
		return matched.stream().map(RegistryKey::clazz)
				.collect(Collectors.toList());
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

	public Class lookupSingle(Class registryPoint, Class targetClass) {
		return lookupSingle(registryPoint, targetClass, false);
	}

	public Class lookupSingle(Class registryPoint, Class targetClass,
			boolean errorOnNull) {
		RegistryKey registryPointKey = keys.get(registryPoint);
		RegistryKey targetClassKey = keys.get(targetClass);
		RegistryKey cachedKey = exactMap.get(registryPointKey, targetClassKey);
		if (cachedKey == null) {
			List<Class> lookup = lookup(true, registryPoint, targetClass,
					false);
			cachedKey = lookup.size() > 0 ? keys.get(lookup.get(0))
					: keys.emptyLookupKey();
			synchronized (exactMap) {
				exactMap.put(registryPoint, targetClass, cachedKey);
			}
		}
		if (cachedKey == keys.emptyLookupKey() && errorOnNull) {
			throw new RegistryException(Ax.format(
					"singleton/factory not registered - %s:%s",
					CommonUtils.classSimpleName(registryPoint),
					CommonUtils.classSimpleName(targetClass)));
		}
		return cachedKey.clazz();
	}

	public void register(Class registeringClass, Class registryPoint) {
		register(registeringClass, registryPoint, void.class,
				ImplementationType.MULTIPLE, 10);
	}

	public void register(Class registeringClass, Class registryPoint,
			Class targetClass, ImplementationType implementationType,
			int infoPriority) {
		RegistryKey registryPointKey = keys.get(registryPoint);
		RegistryKey targetClassKey = keys.get(targetClass);
		RegistryKey registeringClassKey = keys.get(registeringClass);
		register(registeringClassKey, registryPointKey, targetClassKey,
				implementationType, infoPriority);
	}

	/*
	 * In some parts (e.g. assignment to implementationTypeMap) we assume only
	 * one (winning) registering class for
	 */
	public void register(Class registeringClass, RegistryLocation info) {
		register(registeringClass, info.registryPoint(), info.targetClass(),
				info.implementationType(), info.priority());
	}

	public synchronized void register(RegistryKey registeringClassKey,
			RegistryKey registryPointKey, RegistryKey targetClassKey,
			ImplementationType implementationType, int infoPriority) {
		if (implementationType == ImplementationType.NONE) {
			return;
		}
		MultikeyMap<RegistryKey> registered = registry.asMapEnsure(true,
				registryPointKey, targetClassKey);
		UnsortedMultikeyMap<RegistryKey> pointMap = null;
		if (implementationType == ImplementationType.MULTIPLE
				&& targetClassKey == keys.undefinedTargetKey()
				&& infoPriority != RegistryLocation.DEFAULT_PRIORITY) {
			throw new RegistryException(Ax.format(
					"Non-default priority " + "with Multiple impl type -"
							+ " probably should be instance - %s",
					registeringClassKey.name()));
		}
		if (registered.size() == 1
				&& (targetClassKey != keys.undefinedTargetKey()
						|| implementationType != ImplementationType.MULTIPLE)) {
			Integer currentPriority = targetPriority.get(registryPointKey,
					targetClassKey);
			if (currentPriority > infoPriority) {
				return;
			} else {
				registered.clear();
			}
		}
		registered.put(registeringClassKey, registeringClassKey);
		implementationTypeMap.put(registryPointKey, targetClassKey,
				implementationType);
		targetPriority.put(registryPointKey, targetClassKey, infoPriority);
	}

	public void registerBootstrapServices(ClassLookup classLookup) {
		this.classLookup = classLookup;
	}

	public void registerSingleton(Class<?> registryPoint, Class<?> targetClass,
			Object object) {
		registerSingleton(registryPoint, targetClass, object, false);
	}

	public void registerSingleton(Class<?> registryPoint, Class<?> targetClass,
			Object object, boolean removeExisting) {
		registerSingletonInLookups(registryPoint, targetClass, object,
				removeExisting);
		register(object.getClass(), registryPoint, targetClass,
				ImplementationType.SINGLETON, RegistryLocation.MANUAL_PRIORITY);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void shareSingletonMapTo(Registry otherRegistry) {
		otherRegistry.singletons = singletons;
		otherRegistry.voidPointSingletons = voidPointSingletons;
	}

	public void shutdownSingletons() {
		Logger logger = LoggerFactory.getLogger(Registry.class);
		logger.debug("Shutting singletons for registry {}\n{}", name,
				singletons.allValues());
		for (Object o : singletons.allValues()) {
			if (o instanceof RegistrableService) {
				try {
					logger.debug("Shutting down registrable service: {}",
							o.getClass().getName());
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
		sb.append(Ax.format("Class registry: %s\n", name));
		for (RegistryKey c : ((Map<RegistryKey, ?>) registry.delegate())
				.keySet()) {
			sb.append(simpleName(c));
			sb.append(": ");
			int x = 0;
			Map<RegistryKey, MultikeyMap> map = registry.asMap(c).delegate();
			for (RegistryKey c1 : map.keySet()) {
				if (x++ != 0) {
					sb.append(", ");
				}
				sb.append(simpleName(c1));
				sb.append("={");
				int y = 0;
				MultikeyMap multikeyMap = map.get(c1);
				for (RegistryKey c2 : ((Map<RegistryKey, ?>) multikeyMap
						.delegate()).keySet()) {
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
		registry.remove(keys.get(registryPoint), keys.get(targetClass),
				keys.get(registeringClass));
	}

	private synchronized <T> T registerSingletonInLookups(
			Class<?> registryPoint, Class<?> targetClass, T t,
			boolean removeExisting) {
		// double-check we don't have a race
		T existing = (T) singletons.get(keys.get(registryPoint),
				keys.get(targetClass));
		if (existing != null && !removeExisting) {
			return existing;
		}
		boolean voidTarget = targetClass == void.class;
		singletons.put(keys.get(registryPoint), keys.get(targetClass), t);
		if (voidTarget) {
			// use className so we don't have to get class objects from
			// different parts of memory - this did seem to help a jvm
			// optimisation
			voidPointSingletons.put(registryPoint.getName(), t);
		}
		implClassesRegistered.put(t.getClass(), t);
		return t;
	}

	private String simpleName(RegistryKey c) {
		return c == null ? null
				: c.name().contains(".")
						? c.name().substring(c.name().lastIndexOf(".") + 1)
						: c.name();
	}

	protected List<Class> getSuperclassChain(Class targetClass) {
		List<Class> scChain = new ArrayList<Class>();
		Class c = targetClass;
		while (c != null) {
			scChain.add(c);
			c = c.getSuperclass();
		}
		if (!scChain.contains(void.class)) {
			scChain.add(void.class);
		}
		return scChain;
	}

	protected <V> V impl0(Class<V> registryPoint, Class targetClass,
			boolean allowNull) {
		// optimisation
		Object singleton = null;
		RegistryKey registryPointKey = keys.get(registryPoint);
		RegistryKey targetClassKey = keys.get(targetClass);
		boolean voidTarget = keys.isUndefinedTargetKey(targetClassKey);
		if (voidTarget) {
			singleton = voidPointSingletons.get(registryPointKey.name());
		} else {
			singleton = singletons.get(registryPointKey, targetClassKey);
		}
		if (singleton != null && !(singleton instanceof RegistryFactory)) {
			return (V) singleton;
		}
		ImplementationType type = resolveImplementationType(registryPointKey,
				targetClassKey, allowNull);
		Object obj = null;
		if (singleton == null) {
			if (allowNull) {
				obj = instantiateSingleOrNull(registryPoint, targetClass);
				if (obj == null) {
					return null;
				}
			} else {
				obj = instantiateSingle(registryPoint, targetClass);
			}
		}
		type = type == null ? ImplementationType.MULTIPLE : type;
		switch (type) {
		case FACTORY:
			if (singleton == null) {
				singleton = registerSingletonInLookups(registryPoint,
						targetClass, obj, false);
			}
			return (V) ((RegistryFactory) singleton).create(registryPoint,
					targetClass);
		case SINGLETON:
			if (singleton == null) {
				singleton = registerSingletonInLookups(registryPoint,
						targetClass, obj, false);
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
			RegistryKey registryPointKey, RegistryKey targetClassKey,
			boolean allowNull) {
		ImplementationType implementationType = implementationTypeMap
				.get(registryPointKey, targetClassKey);
		if (implementationType != null) {
			return implementationType;
		}
		List<Class> superclassChain = getSuperclassChain(
				targetClassKey.clazz());
		for (Class superclass : superclassChain) {
			implementationType = implementationTypeMap.get(registryPointKey,
					keys.get(superclass));
			if (implementationType != null) {
				implementationTypeMap.put(registryPointKey, targetClassKey,
						implementationType);
				return implementationType;
			}
		}
		if (allowNull) {
			return null;
		}
		String message = Ax.format(
				"Registry: no resolved implementation type for %s :: %s",
				registryPointKey.simpleName(), targetClassKey.simpleName());
		System.out.println(message);
		throw new RegistryException(message);
	}

	protected <T> T singleton0(Class<T> clazz,
			boolean returnNullIfNotRegistered) {
		if (clazz == null) {
			return null;
		}
		T impl = (T) voidPointSingletons.get(clazz.getName());
		if (impl == null && !returnNullIfNotRegistered) {
			if (implClassesRegistered.containsKey(clazz)) {
				throw Ax.runtimeException(
						"Possible double creation of singleton (at different registry points) - class %s",
						clazz.getName());
			}
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

	@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
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
			super(Ax.format(
					"Constructor of singleton %s invoked more than once",
					clazz.getName()));
		}
	}

	public interface RegistryFactory<V> {
		public V create(Class<? extends V> registryPoint, Class targetClass);
	}

	public static interface RegistryProvider {
		void appShutdown();

		Registry getRegistry();
	}
}
