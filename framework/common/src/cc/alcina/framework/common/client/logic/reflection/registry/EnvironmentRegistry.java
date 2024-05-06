package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.LooseContext;

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
 */
public class EnvironmentRegistry extends Registry {
	static final String CONTEXT_REGISTRY = EnvironmentRegistry.class.getName()
			+ ".CONTEXT_REGISTRY";

	static RegistryProvider delegateProvider;

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
				return delegateProvider.getRegistry();
			}
		}
	}

	static Map<Class, Boolean> classEnvironmentSingleton = CollectionCreators.Bootstrap
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
			delegateQuery.classes = classes;
			return delegateQuery;
		}

		@Override
		public boolean hasImplementation() {
			return delegateQuery().hasImplementation();
		}

		@Override
		public V impl() {
			boolean hasEnvironmentSingleton = hasEnvironmentSingleton(type);
			if (hasEnvironmentSingleton) {
				return (V) singletons.ensure(type);
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
			return delegateQuery().optional();
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
}
