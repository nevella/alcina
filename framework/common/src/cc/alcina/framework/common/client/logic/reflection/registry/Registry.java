package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Implementation;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.Registrations.RegistrationData;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;

/**
 * In place of dependency injection and JNDI...we have...the registry
 *
 * <uL>
 * <li>Provide service class implementations, specificied by one or more classes
 * [and possibly a final enum discriminator]
 * <li>List classes sharing a given characteristic
 * </ul>
 *
 * @author nick@alcina.cc
 *
 *         FIXME - synchronize
 *
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class Registry {
	static Registry instance = new Registry();

	public static final String MARKER_RESOURCE = "registry.properties";

	static RegistryProvider provider = new BasicRegistryProvider();

	// must be concurrent if in a concurrent environment
	static DelegateMapCreator delegateCreator = new CollectionCreators.UnsortedMapCreator();

	public static void appShutdown() {
		provider.appShutdown();
	}

	public static Registry get() {
		return instance;
		// provider.getRegistry();
	}

	public static RegistryProvider getProvider() {
		return provider;
	}

	public static <V> V impl(Class<V> type, Class... keys) {
		return get().query0(type).withKeys(keys).impl();
	}

	public static <V> Optional<V> optional(Class<V> type, Class... keys) {
		return get().query0(type).withKeys(keys).optional();
	}

	public static Query query() {
		return get().query0();
	}

	public static <V> Query<V> query(Class<V> type) {
		return get().query0(type);
	}

	public static void setProvider(RegistryProvider provider) {
		Registry.provider = provider;
	}

	Singletons singletons = new Singletons();

	Registrations registrations = new Registrations();

	Implementations implementations = new Implementations();

	RegistryKeys registryKeys = new RegistryKeys();

	public Register register() {
		return new Register();
	}

	public void shareSingletonMapTo(Registry otherRegistry) {
		otherRegistry.singletons = singletons;
	}

	protected Query query0() {
		return new Query();
	}

	protected <V> Query<V> query0(Class<V> type) {
		return new Query<>(type);
	}

	@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
	@Registration(ClearStaticFieldsOnAppShutdown.class)
	public static class BasicRegistryProvider implements RegistryProvider {
		private volatile Registry instance;

		@Override
		public void appShutdown() {
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

	public class Query<V> {
		Class<V> type;

		List<Class> classes = new ArrayList<>();

		public Query() {
		}

		public Query(Class<V> type) {
			this.type = type;
			classes.add(type);
		}

		public List<RegistryKey> asRegistrationKeys() {
			return classes.stream().map(registryKeys::get)
					.collect(Collectors.toList());
		}

		public V impl() {
			return implementations.ensure(this, true);
		}

		public Stream<V> implementations() {
			return typedRegistrations().map(this::subQuery).map(Query::impl);
		}

		public Optional<V> optional() {
			return Optional.ofNullable(implementations.ensure(this, false));
		}

		public Stream<Class> registrations() {
			return registrations.stream(this);
		}

		@Override
		public String toString() {
			return Ax.format("Query: %s", asRegistrationKeys());
		}

		public Stream<Class<? extends V>> typedRegistrations() {
			return (Stream) registrations.stream(this);
		}

		public Query<V> withKeys(Class[] keys) {
			for (Class clazz : keys) {
				classes.add(clazz);
			}
			return this;
		}

		private Query<V> subQuery(Class<? extends V> subKey) {
			Query query = new Query(type);
			query.classes = classes.stream().collect(Collectors.toList());
			query.classes.add(subKey);
			return query;
		}
	}

	public class Register {
		public void add(Class registeringClass, Registration registration) {
			add(registryKeys.get(registeringClass),
					Arrays.stream(registration.value()).map(registryKeys::get)
							.collect(Collectors.toList()),
					registration.implementation(), registration.priority());
		}

		public void add(RegistryKey registeringClassKey, List<RegistryKey> keys,
				Registration.Implementation implementation,
				Registration.Priority priority) {
			registrations.register(registeringClassKey, keys, implementation,
					priority);
		}

		public void singleton(RegistryKey registeringClassKey,
				List<RegistryKey> keys,
				Registration.Implementation implementation,
				Registration.Priority priority) {
			registrations.register(registeringClassKey, keys, implementation,
					priority);
		}
	}

	public interface RegistryFactory<V> {
		public V impl();
	}

	public static interface RegistryProvider {
		void appShutdown();

		Registry getRegistry();
	}

	class Implementations {
		LookupTree<ImplementationData> lookup = new LookupTree<>();

		private boolean ascendSuperclassOfLastKey(List<RegistryKey> keys) {
			RegistryKey key = keys.get(keys.size() - 1);
			Class superclass = key.asSingleClassKey().getSuperclass();
			if (superclass == null) {
				return false;
			} else {
				keys.set(keys.size() - 1, registryKeys.get(superclass));
				return true;
			}
		}

		<V> V ensure(Query<V> query, boolean throwIfNotNull) {
			List<RegistryKey> keys = query.asRegistrationKeys();
			do {
				ImplementationData data = lookup.get(keys);
				if (data == null) {
					Iterator<RegistrationData> itr = registrations
							.registrations(query).stream().sorted().iterator();
					if (itr.hasNext()) {
						RegistrationData first = itr.next();
						if (itr.hasNext()) {
							RegistrationData second = itr.next();
							if (first.priority == second.priority) {
								throw new IllegalStateException(Ax.format(
										"Query: %s - equal top priorities: \n%s",
										query,
										registrations.registrations(query)
												.stream().sorted()
												.map(Object::toString)
												.collect(Collectors
														.joining("\n"))));
							}
						}
						data = new ImplementationData(first);
						lookup.put(keys, data);
					}
				}
				if (data != null) {
					return (V) data.instance();
				}
			} while (ascendSuperclassOfLastKey(keys));
			if (throwIfNotNull) {
				throw new NoSuchElementException(query.toString());
			} else {
				return null;
			}
		}

		class ImplementationData {
			Object registree;

			RegistrationData registrationData;

			public ImplementationData(RegistrationData registrationData) {
				this.registrationData = registrationData;
			}

			public Object instance() {
				Class registeredClass = registrationData.registeringClassKey
						.asSingleClassKey();
				switch (registrationData.implementation) {
				case INSTANCE:
					return Reflections.newInstance(registeredClass);
				case SINGLETON:
					return singletons.get(registeredClass);
				case FACTORY:
					return ((RegistryFactory) singletons.get(registeredClass))
							.impl();
				default:
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	class LookupTree<T> {
		Node root = new Node();

		public void add(List<RegistryKey> keys, Object value) {
			Iterator<RegistryKey> itr = keys.iterator();
			root.add(itr, value);
		}

		public T get(List<RegistryKey> keys) {
			Iterator<RegistryKey> itr = keys.iterator();
			return root.get(itr);
		}

		public Collection<RegistryKey> keys(List<RegistryKey> keys) {
			Iterator<RegistryKey> itr = keys.iterator();
			return root.keys(itr);
		}

		public void put(List<RegistryKey> keys, T t) {
			Iterator<RegistryKey> itr = keys.iterator();
			root.put(itr, t);
		}

		class Node {
			// will be concurrent if in a concurrent environment
			Map<RegistryKey, Node> map = delegateCreator.createDelegateMap(0,
					0);

			T value;

			public void add(Iterator<RegistryKey> itr, Object value) {
				RegistryKey key = itr.next();
				Node child = map.computeIfAbsent(key, k -> {
					Node n = new Node();
					n.value = (T) new ArrayList();
					return n;
				});
				if (itr.hasNext()) {
					child.add(itr, value);
				} else {
					((List) child.value).add(value);
				}
			}

			public T get(Iterator<RegistryKey> itr) {
				RegistryKey key = itr.next();
				Node child = map.get(key);
				if (child == null) {
					return null;
				}
				if (itr.hasNext()) {
					return child.get(itr);
				} else {
					return (T) map.get(key);
				}
			}

			public Collection<RegistryKey> keys(Iterator<RegistryKey> itr) {
				RegistryKey key = itr.next();
				Node child = map.get(key);
				if (itr.hasNext()) {
					return child.keys(itr);
				} else {
					return map.keySet();
				}
			}

			public void put(Iterator<RegistryKey> itr, T t) {
				RegistryKey key = itr.next();
				Node child = map.computeIfAbsent(key, k -> new Node());
				if (itr.hasNext()) {
					child.put(itr, t);
				} else {
					child.value = t;
				}
			}
		}
	}

	class Registrations {
		LookupTree<List<RegistrationData>> lookup = new LookupTree<>();

		void register(RegistryKey registeringClassKey, List<RegistryKey> keys,
				Registration.Implementation implementation,
				Registration.Priority priority) {
			if (implementation == Registration.Implementation.NONE) {
				return;
			}
			lookup.add(keys, new RegistrationData(registeringClassKey,
					implementation, priority));
		}

		List<RegistrationData> registrations(Query query) {
			List<RegistrationData> value = lookup
					.get(query.asRegistrationKeys());
			return value == null ? new ArrayList<>() : value;
		}

		Stream<Class> stream(Query query) {
			Collection<RegistryKey> keys = lookup
					.keys(query.asRegistrationKeys());
			return keys.stream().map(RegistryKey::asSingleClassKey);
		}

		class RegistrationData {
			RegistryKey registeringClassKey;

			Registration.Implementation implementation;

			Registration.Priority priority;

			RegistrationData(RegistryKey registeringClassKey,
					Implementation implementation, Priority priority) {
				this.registeringClassKey = registeringClassKey;
				this.implementation = implementation;
				this.priority = priority;
			}

			@Override
			public String toString() {
				return Ax.format("%s - %s - %s", registeringClassKey,
						implementation, priority);
			}
		}
	}

	class Singletons {
		Map<Class, Object> byClass = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		public Object get(Class singletonClass) {
			return byClass.computeIfAbsent(singletonClass,
					Reflections::newInstance);
		}
	}
}
