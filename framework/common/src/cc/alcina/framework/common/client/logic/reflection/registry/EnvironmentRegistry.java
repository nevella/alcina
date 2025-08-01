package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentOptionalRegistration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Implementation;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

/**
 * <p>
 * The EnvironmentRegistry is an all-layer class designed to support server-side
 * emulation of multiple client (browser) environments - each of which assumes a
 * unique Registry - at least in the sense of certain singletons are unique to
 * the instance.
 * <p>
 * The class mostly routes to the delegate - the wrinkle is that queries must be
 * built and tested in this class before being routed
 * <p>
 * Initially, the per-environment singletons must be manually registered. This
 * could be replaced with marking the per-environment singletons with a
 * particular interface
 * <p>
 * If a class is annotated with {@link Registration.EnvironmentSingleton} and
 * registered with the {@link Registry} - (note the same class is registered for
 * all Environments - that's not configurable) - it will be returned as the
 * singleton for the current environment
 * <p>
 * If a class is annotated with {@link Registration.EnvironmentRegistration} and
 * registered with the {@link Registry} - generally via
 * <code>Registry.register().singleton([Key].class,
					[Impl]);</code> - it will be returned as the singleton for
 * the current environment. So this is the annotation to use for a situation
 * (say TraversalBrowser vs EntityBrowser) where the implementation would be
 * different, depending on the initial context
 */
public class EnvironmentRegistry extends Registry {
	static final String CONTEXT_REGISTRY = EnvironmentRegistry.class.getName()
			+ ".CONTEXT_REGISTRY";

	static RegistryProvider delegateProvider;

	EnvironmentRegister register;

	Registry delegate() {
		return delegateProvider.getRegistry();
	}

	public static void
			registerDelegateProvider(RegistryProvider delegateProvider) {
		RegistryProvider currentProvider = Registry.Internals.getProvider();
		Registry.Internals.setProvider(new Provider());
		EnvironmentRegistry.delegateProvider = delegateProvider != null
				? delegateProvider
				: currentProvider;
	}

	static class Provider implements RegistryProvider {
		@Override
		public void appShutdown() {
			delegateProvider.appShutdown();
		}

		@Override
		public Registry getRegistry() {
			EnvironmentRegistry contextRegistry = LooseContext
					.get(CONTEXT_REGISTRY);
			if (contextRegistry != null) {
				return contextRegistry;
			} else {
				if (delegateProvider == this) {
					return null;
				} else {
					return delegateProvider.getRegistry();
				}
			}
		}
	}

	static Map<Class, Boolean> classEnvironmentSingleton = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	static Map<Class, Boolean> classEnvironmentRegistration = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	public static void enter(EnvironmentRegistry registry) {
		LooseContext.set(CONTEXT_REGISTRY, registry);
	}

	@Override
	protected void init() {
		singletons = new EnvironmentSingletons();
		registrations = delegate().registrations;
		implementations = delegate().implementations;
		registryKeys = delegate().registryKeys;
		register = new EnvironmentRegister(super.register0());
	}

	class EnvironmentQuery<V> extends Registry.Query<V> {
		EnvironmentQuery() {
		}

		EnvironmentQuery(Class<V> type) {
			super(type);
		}

		@Override
		V checkNonSingleton(Class<? extends V> clazz) {
			throw new UnsupportedOperationException();
		}

		Query<V> delegateQuery() {
			Query<V> delegateQuery = delegate().query0();
			delegateQuery.type = type;
			delegateQuery.keys = keys;
			return delegateQuery;
		}

		@Override
		public boolean hasImplementation() {
			return delegateQuery().hasImplementation();
		}

		@Override
		public V impl() {
			boolean hasEnvironmentSingleton = hasEnvironmentSingleton(type);
			boolean hasEnvironmentRegistration = hasEnvironmentRegistration(
					type);
			if (hasEnvironmentSingleton) {
				return (V) singletons.ensure(type);
			} else if (hasEnvironmentRegistration) {
				return (V) register.get(type, keys);
			} else {
				return delegateQuery().impl();
			}
		}

		@Override
		public Stream<V> implementations() {
			return delegateQuery().implementations();
		}

		@Override
		public Optional<V> optional() {
			boolean hasEnvironmentSingleton = hasEnvironmentSingleton(type);
			boolean hasEnvironmentRegistration = hasEnvironmentRegistration(
					type);
			if (hasEnvironmentRegistration || hasEnvironmentSingleton) {
				return Optional.ofNullable(impl());
			} else {
				return delegateQuery().optional();
			}
		}

		@Override
		public Class<? extends V> registration() {
			return delegateQuery().registration();
		}

		@Override
		public Stream<Class<? extends V>> registrations() {
			return delegateQuery().registrations();
		}

		@Override
		Query<V> subQuery(Class<? extends V> subKey) {
			return delegateQuery().subQuery(subKey);
		}

		@Override
		public Stream<Class<?>> untypedRegistrations() {
			return delegateQuery().untypedRegistrations();
		}
	}

	static boolean hasEnvironmentSingleton(Class<?> type) {
		try {
			boolean hasEnvironmentSingleton = classEnvironmentSingleton
					.computeIfAbsent(type, clazz -> Reflections.at(clazz)
							.has(Registration.EnvironmentSingleton.class));
			return hasEnvironmentSingleton;
		} catch (Exception e) {
			return false;
		}
	}

	static boolean hasEnvironmentRegistration(Class<?> type) {
		try {
			boolean hasEnvironmentRegistration = classEnvironmentRegistration
					.computeIfAbsent(type, clazz -> {
						ClassReflector reflector = Reflections.at(clazz);
						boolean has = reflector.has(
								Registration.EnvironmentRegistration.class);
						if (has) {
							/*
							 * Model constraint - the two annotations conflict
							 */
							Preconditions.checkState(!reflector
									.has(Registration.Singleton.class));
						}
						return has;
					});
			return hasEnvironmentRegistration;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected Register register0() {
		return register;
	}

	class EnvironmentRegister extends Register {
		Register delegate;

		EnvironmentRegister(Register delegate) {
			this.delegate = delegate;
		}

		Map<Class<?>, Object> environmentImplementations = AlcinaCollections
				.newLinkedHashMap();

		/*
		 * type, key, impl
		 */
		MultikeyMap<Object> environmentMultikeyImplementations = new UnsortedMultikeyMap<>(
				2);

		// note that type === keys[0]
		public <V> V get(Class<V> type, List<Class> keys) {
			if (keys.size() == 1) {
				return (V) environmentImplementations.get(type);
			} else if (keys.size() == 2) {
				return (V) environmentMultikeyImplementations.get(type,
						keys.get(1));
			} else {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public void add(Class registeringClass, List<Class> keys,
				Implementation implementation, Priority priority) {
			delegate.add(registeringClass, keys, implementation, priority);
		}

		@Override
		public void add(Class registeringClass, Registration registration) {
			delegate.add(registeringClass, registration);
		}

		@Override
		public void add(RegistryKey registeringClassKey, List<RegistryKey> keys,
				Implementation implementation, Priority priority) {
			delegate.add(registeringClassKey, keys, implementation, priority);
		}

		@Override
		public void add(String registeringClassClassName, List<String> keys,
				Implementation implementation, Priority priority) {
			delegate.add(registeringClassClassName, keys, implementation,
					priority);
		}

		@Override
		public void addDefault(Class registeringClass, Class... keys) {
			delegate.addDefault(registeringClass, keys);
		}

		@Override
		public void singleton(Class type, Object implementation) {
			Object existingImplementation = environmentImplementations
					.get(type);
			if (existingImplementation != null) {
				throw new IllegalStateException(Ax.format(
						"Existing implementation: %s :: %s, attempted registration %s",
						NestedName.get(type),
						NestedName.get(existingImplementation),
						NestedName.get(implementation)));
			}
			environmentImplementations.put(type, implementation);
		}

		@Override
		public void singleton(Class type, Class key, Object implementation) {
			Preconditions.checkState(
					!environmentMultikeyImplementations.containsKey(type, key));
			environmentMultikeyImplementations.put(type, key, implementation);
		}
	}

	class EnvironmentSingletons extends Singletons {
		@Override
		<V> V byClass(Class<V> type) {
			if (hasEnvironmentSingleton(type)) {
				return super.byClass(type);
			} else {
				return delegate().singletons.byClass(type);
			}
		}

		@Override
		Object ensure(Class singletonClass) {
			if (hasEnvironmentSingleton(singletonClass)) {
				return super.ensure(singletonClass);
			} else {
				return delegate().singletons.ensure(singletonClass);
			}
		}

		@Override
		void put(Object implementation) {
			if (hasEnvironmentSingleton(implementation.getClass())) {
				super.put(implementation);
			} else {
				delegate().singletons.put(implementation);
			}
		}

		@Override
		void remove(Class singletonImplementationType) {
			if (hasEnvironmentSingleton(singletonImplementationType)) {
				super.remove(singletonImplementationType);
			} else {
				delegate().singletons.remove(singletonImplementationType);
			}
		}
	}

	@Override
	protected Query query0() {
		return new EnvironmentQuery();
	}

	@Override
	protected <V> Query<V> query0(Class<V> type) {
		return new EnvironmentQuery<>(type);
	}

	/**
	 * Register a collection of types for this environment, as singletons, at
	 * points defined by their EnvironmentOptionalRegistration
	 */
	public static void
			registerEnvironmentOptionals(Class<?>... implementationTypes) {
		Arrays.stream(implementationTypes).forEach(implementationType -> {
			ClassReflector<?> reflector = Reflections.at(implementationType);
			EnvironmentOptionalRegistration registration = reflector
					.annotation(EnvironmentOptionalRegistration.class);
			Object impl = reflector.newInstance();
			Registration instanceRegistration = registration.value();
			Class[] keys = instanceRegistration.value();
			Registry.register().singleton(keys[0], keys[1], impl);
		});
	}
}
