package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * In place of dependency injection and JNDI...we have...the registry
 *
 * <uL>
 * <li>Provide service class implementations, specificied by one or more classes
 * [and possibly a final enum discriminator]
 * <li>List classes sharing a given characteristic
 * <li>(Semi-declarative notes): provides implementations satisfying a contract,
 * overridden by context registry
 * <li>TODO - describe implementation resolution, with examples
 * </ul>
 *
 * @author nick@alcina.cc
 *
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

	public static RegistryProvider getProvider() {
		return provider;
	}

	public static <V> V impl(Class<V> type, Class... keys) {
		return get().query0(type).withKeys(keys).impl();
	}

	public static Internals internals() {
		return get().instanceInternals();
	}

	public static <V> Optional<V> optional(Class<V> type, Class... keys) {
		return get().query0(type).withKeys(keys).optional();
	}

	public static Query<?> query() {
		return get().query0();
	}

	public static <V> Query<V> query(Class<V> type) {
		return get().query0(type);
	}

	public static Register register() {
		return get().register0();
	}

	protected static Registry get() {
		return instance;
		// provider.getRegistry();
	}

	Singletons singletons = new Singletons();

	Registrations registrations = new Registrations();

	Implementations implementations = new Implementations();

	RegistryKeys registryKeys = new RegistryKeys();

	public String name;

	public Internals instanceInternals() {
		return new Internals(this);
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

	protected Register register0() {
		return new Register();
	}

	@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
	@Registration(ClearStaticFieldsOnAppShutdown.class)
	public static class BasicRegistryProvider implements RegistryProvider {
		private volatile Registry instance;

		@Override
		public void appShutdown() {
			Internals.setProvider(null);
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

	public static class Internals {
		public static void
				setDelegateCreator(DelegateMapCreator delegateCreator) {
			Registry.delegateCreator = delegateCreator;
		}

		public static void setProvider(RegistryProvider provider) {
			Registry.provider = provider;
		}

		Registry registry;

		public Internals(Registry registry) {
			this.registry = registry;
		}

		public void copyFrom(Registry sourceRegistry, Class<?> type) {
			// copy registrations
			sourceRegistry.registrations
					.registrations(sourceRegistry.query0(type))
					.forEach(rd -> registry.register0().add(
							rd.registeringClassKey,
							Collections.singletonList(
									registry.registryKeys.get(type)),
							rd.implementation, rd.priority));
			// copy singleton
			registry.singletons.put(sourceRegistry.query0(type));
		}

		public void dump() {
			Ax.out("Registry: %s", registry);
			registry.registrations.dump();
		}

		public Registry instance() {
			return registry;
		}

		public void setName(String name) {
			registry.name = name;
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

		/*
		 * For when the first registration key (class) is *not* registered type
		 * (relatively unusual)
		 *
		 * e.g. @Registration({ PersistentImpl.class, ClientInstance.class })
		 *
		 * ...although
		 *
		 * FIXME - reflection.2
		 *
		 * ... that registration should/could be inverted
		 *
		 * also: Registry.query(ContentDelivery.class)
		 * .clearTypeKey().withKeys(ContentDeliveryType.class,
		 * ContentDeliveryType_EMAIL.class) .impl();
		 *
		 * ->
		 *
		 * two-key registration
		 * (ContentDelivery.class,ContentDeliveryType_EMAIL.class)
		 */
		public Query<V> clearTypeKey() {
			classes.clear();
			return this;
		}

		public <E extends Enum> V forEnum(E enumValue) {
			return implementations()
					.filter(e -> ((Registration.EnumDiscriminator) e)
							.provideEnumDiscriminator() == enumValue)
					.findFirst().get();
		}

		public boolean hasImplementation() {
			return implementations.implementation(this, false) != null;
		}

		public V impl() {
			return implementations.ensure(this, true);
		}

		public Stream<V> implementations() {
			return registrations.stream(this).map(this::subQuery)
					.map(Query::impl);
		}

		public Optional<V> optional() {
			return Optional.ofNullable(implementations.ensure(this, false));
		}

		public Class<? extends V> registration() {
			return implementations.implementation(this,
					true).registrationData.registeringClassKey
							.asSingleClassKey();
		}

		public Stream<Class<? extends V>> registrations() {
			return (Stream) untypedRegistrations();
		}

		@Override
		public String toString() {
			return Ax.format("Query: %s", asRegistrationKeys());
		}

		public Stream<Class<?>> untypedRegistrations() {
			return (Stream) registrations.stream(this);
		}

		public Query<V> withKeys(Class... keys) {
			for (Class clazz : keys) {
				classes.add(clazz);
			}
			return this;
		}

		Query<V> subQuery(Class<? extends V> subKey) {
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

		/*
		 * Uses string (className) parameters to avoid class init from cache
		 * loads
		 */
		public void add(String registeringClassClassName, List<String> keys,
				Implementation implementation, Priority priority) {
			add(registryKeys.get(registeringClassClassName), keys.stream()
					.map(registryKeys::get).collect(Collectors.toList()),
					implementation, priority);
		}

		public void singleton(Class type, Object implementation) {
			add(registryKeys.get(implementation.getClass()),
					Collections.singletonList(registryKeys.get(type)),
					Implementation.SINGLETON, Priority.APP);
			singletons.put(implementation);
		}

		public void singleton(Class[] keys, Object implementation) {
			add(registryKeys.get(implementation.getClass()),
					Arrays.stream(keys).map(registryKeys::get)
							.collect(Collectors.toList()),
					Implementation.SINGLETON, Priority.APP);
			singletons.put(implementation);
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
			ImplementationData implementation = implementation(query,
					throwIfNotNull);
			return implementation == null ? null
					: (V) implementation.instance();
		}

		<V> ImplementationData implementation(Query<V> query,
				boolean throwIfNotNull) {
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
					return data;
				}
			} while (ascendSuperclassOfLastKey(keys));
			if (throwIfNotNull) {
				throw new NoSuchElementException(query.toString());
			} else {
				return null;
			}
		}

		class ImplementationData {
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
					return singletons.ensure(registeredClass);
				case FACTORY:
					return ((RegistryFactory) singletons
							.ensure(registeredClass)).impl();
				default:
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	class LookupTree<T> {
		Node root = new Node();

		void add(List<RegistryKey> keys, Object value) {
			Iterator<RegistryKey> itr = keys.iterator();
			root.add(itr, value);
		}

		void dump() {
			root.dump("", 0);
		}

		T get(List<RegistryKey> keys) {
			Iterator<RegistryKey> itr = keys.iterator();
			return root.get(itr);
		}

		Collection<RegistryKey> keys(List<RegistryKey> keys) {
			Iterator<RegistryKey> itr = keys.iterator();
			return root.keys(itr);
		}

		void put(List<RegistryKey> keys, T t) {
			Iterator<RegistryKey> itr = keys.iterator();
			root.put(itr, t);
		}

		class Node {
			// will be concurrent if in a concurrent environment
			Map<RegistryKey, Node> map = delegateCreator.createDelegateMap(0,
					0);

			T value;

			private void dump(String key, int depth) {
				Ax.out("%s : %s", CommonUtils.padStringLeft(key, 30, ' '),
						value);
				map.forEach((k, v) -> v.dump(k.simpleName(), depth + 1));
			}

			void add(Iterator<RegistryKey> itr, Object value) {
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

			T get(Iterator<RegistryKey> itr) {
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

			Collection<RegistryKey> keys(Iterator<RegistryKey> itr) {
				RegistryKey key = itr.next();
				Node child = map.get(key);
				if (itr.hasNext()) {
					return child.keys(itr);
				} else {
					return map.keySet();
				}
			}

			void put(Iterator<RegistryKey> itr, T t) {
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

		void dump() {
			Ax.out("Registrations:\n============");
			lookup.dump();
		}

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
		private Map<Class, Object> byClass = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		boolean contains(Class singletonClass) {
			return byClass.containsKey(singletonClass);
		}

		Object ensure(Class singletonClass) {
			return byClass.computeIfAbsent(singletonClass,
					Reflections::newInstance);
		}

		void put(Object implementation) {
			Class<? extends Object> clazz = implementation.getClass();
			if (byClass.containsKey(clazz)) {
				throw new IllegalStateException(
						Ax.format("Existing registration of singleton - %s",
								clazz.getName()));
			}
			byClass.put(clazz, implementation);
		}
	}
}
