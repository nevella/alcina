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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.PropertyValueMapper;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
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
 *         // Registration.Implementation.INSTANCE required for registry override
 *         &#64;RegistryLocation(registryPoint = DoSomethingFunky.class, implementation = Registration.Implementation.INSTANCE)
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
 *         &#64;RegistryLocation(registryPoint = DoSomethingFunky.class, implementation = Registration.Implementation.INSTANCE, priority = RegistryLocation.MANUAL_PRIORITY)
 *         public class DoSomethingFunkyJvmImpl {
 *         	public void justDoIt() {
 *         		// do incredible jvm-only deeds
 *         	}
 *         }
 *         </pre>
 *
 *         FIXME - dirndl.1a - the registrylocation->registration/keys (and dox)
 *
 *         Thread-safety: operations which can logically conflict (register(),
 *         singleton creation) are synchronized on the instance - or the
 *         registrykey if possible. All maps are concurrent in a threaded
 *         environment
 *
 *         Document: at the moment Registrations can have at most two classes -
 *         fairly easy to extend to arbitrary if helpful (just a few
 *         Precondition checks in this class document where)
 *
 *         FIXME - reflection - remove 'targetClass' usage
 *
 *         FIXME - again:
 *
 *         - have a simpler public api - impl, types, query
 *
 *         - make more o/o - the class is too flat,
 *
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class Registry0 {
	public static final String MARKER_RESOURCE = "registry.properties";
	// private static RegistryProvider provider = new BasicRegistryProvider();

	// must be concurrent if in a concurrent environment
	static DelegateMapCreator delegateCreator = new CollectionCreators.UnsortedMapCreator();
	// public static void appShutdown() {
	// provider.appShutdown();
	// }

	public static <T> T checkSingleton(Class<T> clazz) {
		return get().singleton0(clazz, true);
	}

	public static void checkSingleton(RegistrySingleton singleton) {
		if (Registry0.get().singleClassKeySingletons
				.containsKey(singleton.getClass().getName())) {
			throw new MultipleSingletonException(singleton.getClass());
		}
	}

	public static <T> T ensureSingleton(Class<T> clazz) {
		return get().singleton0(clazz, false);
	}

	public static Registry0 get() {
		return null;// provider.getRegistry();
	}
	// public static RegistryProvider getProvider() {
	// return provider;
	// }

	public static boolean hasImpl(Class<?> type) {
		return Registry0.implOrNull(type) != null;
	}

	public static <V> V impl(Class<V> type) {
		return get().impl0(type, false);
	}

	public static <V> V impl(Class<V> type, boolean allowNull, Class... keys) {
		return get().impl0(type, allowNull, keys);
	}

	public static <V> V impl(Class<V> type, Class... keys) {
		return get().impl0(type, false, keys);
	}

	public static <V> Optional<V> implOptional(Class<V> type) {
		return Optional.<V> ofNullable(implOrNull(type));
	}

	public static <V> V implOrNull(Class<V> type, Class... keys) {
		return get().impl0(type, true, keys);
	}

	public static <V> List<V> impls(Class<V> type) {
		return impls(type);
	}

	public static <V> List<V> impls(Class<V> type, Class targetClass) {
		return get().impls0(type, targetClass);
	}

	public static <T> Optional<T> optional(Class<T> type) {
		return Optional.ofNullable(implOrNull(type));
	}

	public static <T> Optional<T> optional(Class<T> type, Class targetClass) {
		return Optional.ofNullable(implOrNull(type, targetClass));
	}

	public static void registerSingleton(Object object, Class<?> type,
			boolean removeExisting, Class<?>... keys) {
		get().registerSingletonInLookups(type, object, removeExisting, keys);
		get().register(object.getClass(), type,
				Registration.Implementation.SINGLETON,
				Registration.Priority.APP, keys);
	}

	public static void registerSingleton(Object object, Class<?> type,
			Class<?>... keys) {
		registerSingleton(object, type, false, keys);
	}

	public static void setDelegateCreator(DelegateMapCreator delegateCreator) {
		Registry.delegateCreator = delegateCreator;
	}

	public static <V> List<V> singletons(Class<V> type, Class targetClass) {
		return get().singletons0(type, targetClass);
	}

	private String name;

	private RegistryKeys registryKeys;

	// type/secondariesKey/impl/impl
	protected UnsortedMultikeyMap<RegistrationData> registry;

	// typeKey/secondariesKey/exact-match-class
	// TODO - document
	protected UnsortedMultikeyMap<RegistryKey> exactMap;

	// registrypoint/targetClass/singleton
	protected UnsortedMultikeyMap<Object> singletons;

	// FIXME - reflection - singleClassKey
	protected Map<String, Object> singleClassKeySingletons;

	private Map<Class, Object> implClassesRegistered;

	public Registry0() {
		registryKeys = new RegistryKeys();
		registry = new UnsortedMultikeyMap<>(3, 0, delegateCreator);
		singletons = new UnsortedMultikeyMap<Object>(2, 0, delegateCreator);
		singleClassKeySingletons = delegateCreator.createDelegateMap(0, 0);
		exactMap = new UnsortedMultikeyMap<>(2, 0, delegateCreator);
		implClassesRegistered = delegateCreator.createDelegateMap(0, 0);
	}

	public List<Class> allImplementationKeys(Class type) {
		RegistryKey registryPointKey = registryKeys.get(type);
		MultikeyMap<RegistryKey> pointLookup = registry.asMapEnsure(false,
				registryPointKey);
		if (pointLookup == null) {
			return Collections.emptyList();
		}
		return pointLookup.typedKeySet(RegistryKey.class).stream()
				.map(key -> key.asSingleClassKey()).filter(Objects::nonNull)
				.filter(clazz -> clazz != void.class)
				.collect(Collectors.toList());
	}

	public void copyFrom(Registry0 sourceInstance, Class<?> type) {
		RegistryKey key = registryKeys.get(type);
		registry.asMap(key).putMulti(sourceInstance.registry.asMap(key));
		exactMap.asMap(key).putMulti(sourceInstance.exactMap.asMap(key));
		sourceInstance.registry.asMap(key).typedKeySet(RegistryKey.class)
				.forEach(childKey -> sourceInstance.impl0(type, false,
						((RegistryKey) childKey).classes()));
		if (sourceInstance.singletons.containsKey(key)) {
			singletons.asMap(key)
					.putMulti(sourceInstance.singletons.asMap(key));
			String cn = key.name();
			if (singleClassKeySingletons.containsKey(cn)) {
				singleClassKeySingletons.put(cn,
						sourceInstance.singleClassKeySingletons.get(cn));
			}
		}
	}

	public <T> void ensureSingletonRegistered(Class<? super T> clazz, T t) {
		if (impl0(clazz, true) == null) {
			registerSingleton(t, clazz);
		}
	}

	// FIXME - reflection - replace with RegistrationDiscriminator
	public <T> Map<Enum, T> enumLookup(Class<T> type, String propertyName) {
		List<T> handlers = Registry0.impls(type);
		Map<Enum, T> byKey = new LinkedHashMap<>();
		PropertyValueMapper<T, Object> mapper = new PropertyValueMapper<>(type,
				propertyName);
		for (T handler : handlers) {
			Enum key = (Enum) mapper.apply(handler);
			if (byKey.containsKey(key)) {
				throw new RuntimeException(
						Ax.format("Duplicate key for enum lookup - %s %s %s",
								type.getClass().getSimpleName(), key,
								handler.getClass().getSimpleName()));
			} else {
				byKey.put(key, handler);
			}
		}
		return byKey;
	}

	public UnsortedMultikeyMap<RegistrationData> getRegistry() {
		return this.registry;
	}

	public Object instantiateSingle(Class type, Class... keys) {
		Class lookupSingle = lookupSingle(type, true, keys);
		return Reflections.newInstance(lookupSingle);
	}

	public Object instantiateSingleOrNull(Class type, Class... keys) {
		Class lookupSingle = lookupSingle(type, false, keys);
		return lookupSingle != Void.class && lookupSingle != null
				? instantiateSingle(type, keys)
				: null;
	}

	public RegistryKey key(String className) {
		return registryKeys.get(className);
	}

	public List<Class> lookup(boolean mostSpecificTarget, Class type,
			boolean required, Class... keys) {
		Preconditions.checkArgument(keys.length == 1);
		RegistryKey typeKey = registryKeys.get(type);
		// superclasschain
		List<Class> scChain = getSuperclassChain(keys[0]);
		Set<RegistryKey> matched = new LinkedHashSet<>();
		MultikeyMap<RegistryKey> pointLookup = registry.asMapEnsure(false,
				typeKey);
		if (pointLookup == null) {
			if (!required) {
				return new ArrayList<>(0);
			}
			throw new RuntimeException(
					Ax.format("Unable to locate %s - %s", type, keys[0]));
		}
		for (Class sc : scChain) {
			RegistryKey pointKey = registryKeys.get(sc);
			Collection<RegistryKey> targetKeys = pointLookup.keys(pointKey);
			if (targetKeys != null) {
				matched.addAll(targetKeys);
				if (mostSpecificTarget && pointLookup.size() != 0) {
					break;
				}
			}
		}
		return matched.stream().map(key -> key.asSingleClassKey())
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	public List<Class> lookup(Class type) {
		return lookup(false, type, true);
	}

	public <T> T lookupImplementation(Class<T> type, Enum value,
			String propertyName) {
		return lookupImplementation(type, value, propertyName, false);
	}

	public <T> T lookupImplementation(Class<T> type, Enum value,
			String propertyName, boolean newInstance) {
		Map<Enum, T> byKey = enumLookup(type, propertyName);
		T t = byKey.get(value);
		if (t != null && newInstance) {
			try {
				t = (T) Reflections.newInstance(t.getClass());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return t;
	}

	public Class lookupSingle(Class type) {
		return lookupSingle(type, false);
	}

	public Class lookupSingle(Class type, boolean errorOnNull, Class... keys) {
		Preconditions.checkArgument(keys.length == 1);
		RegistryKey typeKey = registryKeys.get(type);
		RegistryKey secondaryKey = registryKeys.get(keys);
		RegistryKey cachedKey = exactMap.get(typeKey, secondaryKey);
		if (cachedKey == null) {
			List<Class> lookup = lookup(true, type, false, keys);
			cachedKey = lookup.size() > 0 ? registryKeys.get(lookup.get(0))
					: registryKeys.emptyLookupKey();
			exactMap.put(typeKey, secondaryKey, cachedKey);
		}
		if (cachedKey == registryKeys.emptyLookupKey() && errorOnNull) {
			// throw new RegistryException(this,
			// Ax.format("singleton/factory not registered - %s:%s",
			// CommonUtils.classSimpleName(type),
			// CommonUtils.classSimpleName(keys[0])));
			throw new UnsupportedOperationException();
		}
		return cachedKey.asSingleClassKey();
	}

	public void register(Class registeringClass, Class type,
			Registration.Implementation implementation,
			Registration.Priority priority, Class... keys) {
		RegistryKey typeKey = registryKeys.get(type);
		RegistryKey secondaryKeys = registryKeys.get(keys);
		RegistryKey registeringClassKey = registryKeys.get(registeringClass);
		register(registeringClassKey, typeKey, secondaryKeys, implementation,
				priority);
	}

	/*
	 * In some parts (e.g. assignment to implementationMap) we assume only one
	 * (winning) registering class for
	 */
	public void register(Class registeringClass, Registration registration) {
		register(registryKeys.get(registeringClass),
				registryKeys.getFirst(registration.value()),
				registryKeys.getNonFirst(registration.value()),
				registration.implementation(), registration.priority());
	}

	public synchronized void register(RegistryKey registeringClassKey,
			RegistryKey typeKey, RegistryKey secondaryKey,
			Registration.Implementation implementation,
			Registration.Priority priority) {
		if (implementation == Registration.Implementation.NONE) {
			return;
		}
		MultikeyMap<RegistrationData> registered = registry.asMapEnsure(true,
				typeKey, secondaryKey);
		registered.put(registeringClassKey, registeringClassKey);
	}

	public void setName(String name) {
		this.name = name;
	}
	// public void shareSingletonMapTo(Registry otherRegistry) {
	// otherRegistry.singletons = singletons;
	// otherRegistry.singleClassKeySingletons = singleClassKeySingletons;
	// }

	public void shutdownSingletons() {
		Logger logger = LoggerFactory.getLogger(Registry.class);
		logger.debug("CLearing singletons for registry {}\n{}", name,
				singletons.allValues());
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

	public void unregister(Class type, Class targetClass,
			Class registeringClass) {
		registry.remove(registryKeys.get(type), registryKeys.get(targetClass),
				registryKeys.get(registeringClass));
	}

	private <V> V impl1(Class<V> type,
			Registration.Implementation implementation, boolean allowNull,
			Class... secondaryKeys) {
		Object impl = null;
		if (allowNull) {
			impl = instantiateSingleOrNull(type, secondaryKeys);
			if (impl == null) {
				return null;
			}
		} else {
			impl = instantiateSingle(type, secondaryKeys);
		}
		implementation = implementation == null
				? Registration.Implementation.INSTANCE
				: implementation;
		switch (implementation) {
		case FACTORY: {
			Object singleton = registerSingletonInLookups(type, impl, false,
					secondaryKeys);
			return ((RegistryFactory<V>) singleton).impl();
		}
		case SINGLETON: {
			Object singleton = registerSingletonInLookups(type, impl, false,
					secondaryKeys);
			return (V) singleton;
		}
		case INSTANCE:
			// nothing fancy
			return (V) impl;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private synchronized <T> T registerSingletonInLookups(Class<?> type, T t,
			boolean removeExisting, Class<?>... secondaries) {
		// double-check we don't have a race
		RegistryKey secondariesKey = registryKeys.get(secondaries);
		T existing = (T) singletons.get(registryKeys.get(type), secondariesKey);
		if (existing != null && !removeExisting) {
			return existing;
		}
		boolean voidTarget = secondariesKey == registryKeys
				.undefinedTargetKey();
		singletons.put(registryKeys.get(type), secondariesKey, t);
		if (voidTarget) {
			// use className so we don't have to get class objects from
			// different parts of memory - this did seem to help a jvm
			// optimisation
			singleClassKeySingletons.put(type.getName(), t);
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

	/*
	 *
	 */
	protected <V> V impl0(Class<V> type, boolean allowNull,
			Class... secondaryKeys) {
		RegistryKey typeKey = registryKeys.get(type);
		RegistryKey secondaryKey = registryKeys.get(secondaryKeys);
		boolean voidTarget = registryKeys.isUndefinedTargetKey(secondaryKey);
		Object singleton = null;
		if (voidTarget) {
			singleton = singleClassKeySingletons.get(typeKey.name());
		} else {
			singleton = singletons.get(typeKey, secondaryKey);
		}
		if (singleton != null) {
			if (singleton instanceof RegistryFactory) {
				return ((RegistryFactory<V>) singleton).impl();
			} else {
				return (V) singleton;
			}
		}
		Registration.Implementation implementation = resolveImplementation(
				typeKey, secondaryKey, allowNull);
		if (implementation == null) {
			// only here if allowNull==true
			return null;
		}
		switch (implementation) {
		case SINGLETON:
		case FACTORY:
			// synchronize to prevent double-creation
			synchronized (typeKey) {
				if (voidTarget) {
					singleton = singleClassKeySingletons.get(typeKey.name());
				} else {
					singleton = singletons.get(typeKey, secondaryKey);
				}
				if (singleton != null) {
					return (V) singleton;
				}
				return impl1(type, implementation, allowNull, secondaryKeys);
			}
		default:
			return impl1(type, implementation, allowNull, secondaryKeys);
		}
	}

	protected <V> List<V> impls0(Class<V> type, Class... secondaryKeys) {
		List<Class> impls = lookup(false, type, false, secondaryKeys);
		List<V> result = new ArrayList<V>();
		for (Class c : impls) {
			V impl = (V) impl(c, true);
			result.add(impl != null ? impl : (V) Reflections.newInstance(c));
		}
		return result;
	}

	protected <V> Registration.Implementation resolveImplementation(
			RegistryKey registryPointKey, RegistryKey targetClassKey,
			boolean allowNull) {
		throw new UnsupportedOperationException();
		// Registration.Implementation implementation = implementationMap
		// .get(registryPointKey, targetClassKey);
		// if (implementation != null) {
		// return implementation;
		// }
		// List<Class> superclassChain = getSuperclassChain(
		// targetClassKey.clazz());
		// for (Class superclass : superclassChain) {
		// implementation = implementationMap.get(registryPointKey,
		// registryKeys.get(superclass));
		// if (implementation != null) {
		// implementationMap.put(registryPointKey, targetClassKey,
		// implementation);
		// return implementation;
		// }
		// }
		// if (allowNull) {
		// return null;
		// }
		// String message = Ax.format(
		// "Registry: no resolved implementation type for %s :: %s",
		// registryPointKey.simpleName(), targetClassKey.simpleName());
		// System.out.println(message);
		// throw new RegistryException(this, message);
	}

	protected <T> T singleton0(Class<T> clazz,
			boolean returnNullIfNotRegistered) {
		if (clazz == null) {
			return null;
		}
		T impl = (T) singleClassKeySingletons.get(clazz.getName());
		if (impl == null && !returnNullIfNotRegistered) {
			if (implClassesRegistered.containsKey(clazz)) {
				throw Ax.runtimeException(
						"Possible double creation of singleton (at different registry points) - class %s",
						clazz.getName());
			}
			impl = Reflections.newInstance(clazz);
			registerSingleton(impl, clazz);
		}
		return impl;
	}

	protected <V> List<V> singletons0(Class<V> type,
			Class... secondaryClasses) {
		List<Class> impls = get().lookup(false, type, false, secondaryClasses);
		List<V> result = new ArrayList<V>();
		for (Class c : impls) {
			result.add((V) ensureSingleton(c));
		}
		return result;
	}
	// @RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
	// @Registration(ClearStaticFieldsOnAppShutdown.class)
	// public static class BasicRegistryProvider implements RegistryProvider {
	// private volatile Registry instance;
	//
	// @Override
	// public void appShutdown() {
	// Registry.setProvider(null);
	// }
	//
	// @Override
	// public Registry getRegistry() {
	// if (instance == null) {
	// synchronized (this) {
	// instance = new Registry();
	// }
	// }
	// return instance;
	// }
	// }

	public static class MultipleSingletonException extends RuntimeException {
		public MultipleSingletonException(Class<?> clazz) {
			super(Ax.format(
					"Constructor of singleton %s invoked more than once",
					clazz.getName()));
		}
	}

	public static class RegisteredSingletonInfo {
		public Class<? extends Object> class1;

		public Object t;

		public RegistryKey registeringClassKey;

		public RegistryKey registryPointKey;

		public RegistryKey targetClassKey;

		public Registration.Implementation implementation;

		public Registration.Priority priority;

		public RegisteredSingletonInfo(Class<? extends Object> class1,
				Object t) {
			this.class1 = class1;
			this.t = t;
		}

		public RegisteredSingletonInfo(RegistryKey registeringClassKey,
				RegistryKey registryPointKey, RegistryKey targetClassKey,
				Registration.Implementation implementation,
				Registration.Priority priority) {
			this.registeringClassKey = registeringClassKey;
			this.registryPointKey = registryPointKey;
			this.targetClassKey = targetClassKey;
			this.implementation = implementation;
			this.priority = priority;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("class1");
			sb.append(":");
			sb.append(class1);
			sb.append("\n");
			sb.append("t");
			sb.append(":");
			sb.append(t);
			sb.append("\n");
			sb.append("registeringClassKey");
			sb.append(":");
			sb.append(registeringClassKey);
			sb.append("\n");
			sb.append("registryPointKey");
			sb.append(":");
			sb.append(registryPointKey);
			sb.append("\n");
			sb.append("targetClassKey");
			sb.append(":");
			sb.append(targetClassKey);
			sb.append("\n");
			sb.append("implementation");
			sb.append(":");
			sb.append(implementation);
			sb.append("\n");
			sb.append("priority");
			sb.append(":");
			sb.append(priority);
			sb.append("\n");
			return sb.toString();
		}
	}

	public interface RegistryFactory<V> {
		public V impl();
	}

	public static interface RegistryProvider {
		void appShutdown();

		Registry getRegistry();
	}

	class RegistrationData implements Comparable<RegistrationData> {
		RegistryKey registeringClassKey;

		Registration.Implementation implementation;

		Registration.Priority priority;

		@Override
		/*
		 * First is highest priority
		 */
		public int compareTo(RegistrationData o) {
			return -(priority.compareTo(o.priority));
		}
	}
}
