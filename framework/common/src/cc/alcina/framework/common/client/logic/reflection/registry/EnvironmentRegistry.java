package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.Optional;
import java.util.stream.Stream;

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
	static RegistryProvider delegateProvider;

	Registry delegate() {
		return delegateProvider.getRegistry();
	}

	public static void registerProviders() {
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
			return delegateQuery().impl();
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

	class EnvironmentSingletons extends Singletons {
		@Override
		<V> V byClass(Class<V> type) {
			V result = super.byClass(type);
			if (result != null) {
				return result;
			}
			return delegate().singletons.byClass(type);
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
