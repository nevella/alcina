package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Implementation;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.Registrations.RegistrationData;
import cc.alcina.framework.common.client.reflection.ClassReflector;
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
 * <li>TODO - describe per-classloader (war, entity jar) registry relationship
 * rationale
 * </ul>
 *
 * @author nick@alcina.cc
 *
 *
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class Registry {
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

	public static <V> V impl(Class<V> type) {
		return get().query0(type).impl();
	}

	public static <V> V impl(Class<V> type, Class... keys) {
		return get().query0(type).addKeys(keys).impl();
	}

	public static Internals internals() {
		return get().instanceInternals();
	}

	public static <V> Optional<V> optional(Class<V> type) {
		return get().query0(type).optional();
	}

	public static <V> Optional<V> optional(Class<V> type, Class... keys) {
		return get().query0(type).addKeys(keys).optional();
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
		return provider.getRegistry();
	}

	Singletons singletons = new Singletons();

	Registrations registrations = new Registrations();

	Implementations implementations = new Implementations();

	RegistryKeys registryKeys = new RegistryKeys();

	public String name;

	Registry sharedImplementations;

	Logger logger = LoggerFactory.getLogger(getClass());

	public Internals instanceInternals() {
		return new Internals(this);
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
		public static List<Registration> removeNonImplmentationRegistrations(
				List<Registration> registrations,
				Predicate<Registration> permitEqualPriorityTest) {
			List<Registration> result = new ArrayList<>();
			LookupTree<List<Registration>> lookup = new LookupTree<>();
			registrations.forEach(r -> lookup.add(Arrays.stream(r.value())
					.map(RegistryKey::new).collect(Collectors.toList()), r));
			List<List<Registration>> allValues = lookup.allValues();
			return allValues.stream().filter(Objects::nonNull).filter(list -> {
				if (list.size() > 1) {
					List<Priority> priorities = list.stream()
							.map(r -> r.priority())
							.sorted(Comparator.reverseOrder())
							.collect(Collectors.toList());
					// equal priorities, not an enumdiscriminator list
					if (priorities.get(0) == priorities.get(1)
							&& !permitEqualPriorityTest.test(list.get(0))) {
						return false;
					} else {
						return true;
					}
				} else {
					return true;
				}
			}).flatMap(Collection::stream).collect(Collectors.toList());
		}

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

		public void shareImplementationsTo(Registry otherRegistry) {
			otherRegistry.sharedImplementations = registry;
			otherRegistry.singletons = registry.singletons;
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

		public Query<V> addKeys(Class... keys) {
			for (Class clazz : keys) {
				classes.add(clazz);
			}
			return this;
		}

		public List<RegistryKey> asRegistrationKeys() {
			return classes.stream().map(registryKeys::get)
					.collect(Collectors.toList());
		}

		public Stream<Class<?>> childKeys() {
			return (Stream) registrations.keys(this).stream()
					.map(RegistryKey::clazz);
		}

		public <E extends Enum> V forEnum(E enumValue) {
			return implementations()
					.filter(e -> ((Registration.EnumDiscriminator) e)
							.provideEnumDiscriminator() == enumValue)
					.findFirst().orElse(null);
		}

		public boolean hasImplementation() {
			return implementations.implementation(this, false) != null;
		}

		public V impl() {
			return implementations.ensure(this, true);
		}

		public Stream<V> implementations() {
			return (Stream) registrations.stream(this)
					.map(this::checkNonSingleton);
		}

		public Optional<V> optional() {
			return Optional.ofNullable(implementations.ensure(this, false));
		}

		public Class<? extends V> registration() {
			return implementations.implementation(this,
					true).registrationData.registeringClassKey.clazz();
		}

		public Stream<Class<? extends V>> registrations() {
			return (Stream) untypedRegistrations();
		}

		/*
		 * For when the first registration key (class) is *not* the registered
		 * type (relatively unusual)
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
		public Query<V> setKeys(Class... keys) {
			classes.clear();
			return addKeys(keys);
		}

		@Override
		public String toString() {
			return Ax.format("Query: %s", asRegistrationKeys());
		}

		public Stream<Class<?>> untypedRegistrations() {
			return (Stream) registrations.stream(this);
		}

		V checkNonSingleton(Class<? extends V> clazz) {
			Preconditions.checkArgument(!singletons.contains(clazz));
			return Reflections.newInstance(clazz);
		}

		Query<V> subQuery(Class<? extends V> subKey) {
			Query query = new Query(type);
			query.classes = classes.stream().collect(Collectors.toList());
			query.classes.add(subKey);
			return query;
		}
	}

	public class Register {
		public void add(Class registeringClass, List<Class> keys,
				Registration.Implementation implementation,
				Registration.Priority priority) {
			registrations
					.register(registryKeys.get(registeringClass),
							keys.stream().map(registryKeys::get)
									.collect(Collectors.toList()),
							implementation, priority);
		}

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

		// FIXME - reflection.2 - trim usage
		public void singleton(Class type, Object implementation) {
			if (sharedImplementations != null) {
				sharedImplementations.register0().singleton(type,
						implementation);
			} else {
				RegistryKey typeKey = registryKeys.get(type);
				if (implementations.exists(typeKey)) {
					throw new IllegalStateException(Ax.format(
							"Registering %s at key %s - existing registration",
							implementation, typeKey));
				}
				Preconditions.checkState(!implementations.exists(typeKey));
				registrations.clear(typeKey);
				Implementation implementationType = implementation instanceof RegistryFactory
						? Implementation.FACTORY
						: Implementation.SINGLETON;
				add(registryKeys.get(implementation.getClass()),
						Collections.singletonList(typeKey), implementationType,
						Priority.APP);
				singletons.put(implementation);
			}
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

		<V> V ensure(Query<V> query, boolean throwIfNotNull) {
			ImplementationData implementation = implementation(query,
					throwIfNotNull);
			return implementation == null ? null
					: (V) implementation.instance();
		}

		boolean exists(RegistryKey key) {
			return lookup.root.map.containsKey(key);
		}

		<V> ImplementationData implementation(Query<V> query,
				boolean throwIfNotNull) {
			List<RegistryKey> keys = query.asRegistrationKeys();
			ImplementationData data = lookup.get(keys);
			if (data != null) {
				// fast path
				return data;
			}
			KeyAscent ascent = new KeyAscent(keys);
			do {
				data = lookup.get(keys);
				if (data == null) {
					if (sharedImplementations != null) {
						data = sharedImplementations.implementations
								.implementation(query, throwIfNotNull);
						return data;
					}
					List<RegistrationData> located = registrations
							.registrations(ascent.keys);
					Iterator<RegistrationData> itr = located.stream().sorted()
							.iterator();
					if (itr.hasNext()) {
						RegistrationData first = itr.next();
						if (itr.hasNext()) {
							RegistrationData second = itr.next();
							if (first.priority == second.priority) {
								if (first.registeringClassKey == second.registeringClassKey
										&& located.size() == 2) {
									logger.warn(
											"Duplicate registration of same class (probably fragment/split issue):\n{}",
											located);
								} else {
									throw new IllegalStateException(Ax.format(
											"Query: %s - resolved keys: %s - equal top priorities: \n%s",
											query, ascent.keys,
											located.stream().sorted()
													.map(Object::toString)
													.collect(Collectors
															.joining("\n"))));
								}
							}
						}
						data = new ImplementationData(first);
						lookup.put(ascent.initialKeys, data);
					}
				}
				if (data != null) {
					return data;
				}
			} while (ascent.ascend());
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
						.clazz();
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

		class KeyAscent {
			List<RegistryKey> keys;

			boolean ascendedFinalKey;

			List<RegistryKey> initialKeys;

			public KeyAscent(List<RegistryKey> keys) {
				this.keys = keys;
				this.initialKeys = keys.stream().collect(Collectors.toList());
			}

			/*
			 * @formatter:off
			 *
			 * When resolving implementation for keys (A,B), resolution is:
			 *
			 * * match (A,B)
			 * * match (A,X) (X super B)
			 * * match (A)
			 *
			 * * *do not* match (Y) (Y super A)
			 *
			 * @formatter:on
			 */
			boolean ascend() {
				if (keys.size() == 1) {
					return false;
				}
				RegistryKey key = keys.get(keys.size() - 1);
				Class superclass = key.clazz().getSuperclass();
				if (superclass == null) {
					if (ascendedFinalKey) {
						return false;
					}
					keys = keys.subList(0, keys.size() - 1);
					ascendedFinalKey = true;
				} else {
					if (ascendedFinalKey) {
						return false;
					} else {
						keys.set(keys.size() - 1, registryKeys.get(superclass));
					}
				}
				return true;
			}
		}
	}

	static class LookupTree<T> {
		Node root = new Node();

		void add(List<RegistryKey> keys, Object value) {
			Iterator<RegistryKey> itr = keys.iterator();
			root.add(itr, value);
		}

		List<T> allValues() {
			List<T> result = new ArrayList<>();
			Stack<Node> stack = new Stack<>();
			stack.push(root);
			while (stack.size() > 0) {
				Node node = stack.pop();
				result.add(node.value);
				node.map.values().forEach(stack::add);
			}
			return result;
		}

		void clear(RegistryKey key) {
			root.map.remove(key);
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
				Ax.out("%s : %s", CommonUtils.padStringRight(key, 45, ' '),
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
					return map.get(key).value;
				}
			}

			Collection<RegistryKey> keys(Iterator<RegistryKey> itr) {
				RegistryKey key = itr.next();
				Node child = map.get(key);
				if (itr.hasNext()) {
					return child.keys(itr);
				} else {
					return child == null ? Collections.emptyList()
							: child.map.keySet();
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

		public void clear(RegistryKey typeKey) {
			lookup.clear(typeKey);
		}

		public Collection<RegistryKey> keys(Query query) {
			return lookup.keys(query.asRegistrationKeys());
		}

		void dump() {
			Ax.out("Registrations:\n============");
			lookup.dump();
		}

		void register(RegistryKey registeringClassKey, List<RegistryKey> keys,
				Registration.Implementation implementation,
				Registration.Priority priority) {
			Preconditions.checkArgument(priority != Priority.REMOVE);
			lookup.add(keys, new RegistrationData(registeringClassKey,
					implementation, priority));
		}

		List<RegistrationData> registrations(List<RegistryKey> keys) {
			List<RegistrationData> value = lookup.get(keys);
			return value == null ? new ArrayList<>() : value;
		}

		Stream<Class> stream(Query<?> query) {
			return registrations(query.asRegistrationKeys()).stream()
					.map(RegistrationData::getRegisteringClassKey)
					.map(RegistryKey::clazz)
					// FIXME - reflection - patch double-registration (legacy
					// client modules?)
					.distinct();
		}

		class RegistrationData implements Comparable<RegistrationData> {
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
			/*
			 * Order: highest to lowest priority
			 */
			public int compareTo(RegistrationData o) {
				return -(priority.compareTo(o.priority));
			}

			@Override
			public String toString() {
				return Ax.format("%s - %s - %s", registeringClassKey,
						implementation, priority);
			}

			RegistryKey getRegisteringClassKey() {
				return this.registeringClassKey;
			}
		}
	}

	class RegistryKeys {
		Map<String, RegistryKey> keys = CollectionCreators.Bootstrap
				.createConcurrentStringMap();

		RegistryKeys() {
		}

		RegistryKey get(Class clazz) {
			String name = clazz.getName();
			return keys.computeIfAbsent(name, k -> new RegistryKey(clazz));
		}

		RegistryKey get(String name) {
			return keys.computeIfAbsent(name, RegistryKey::new);
		}
	}

	class Singletons {
		private Map<Class, Object> byClass = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		boolean contains(Class singletonClass) {
			return byClass.containsKey(singletonClass);
		}

		Object ensure(Class singletonClass) {
			// can return IllegalStateException due to recursive singleton
			// creation
			// return byClass.computeIfAbsent(singletonClass,
			// Reflections::newInstance);
			Object value = byClass.get(singletonClass);
			if (value != null) {
				return value;
			}
			// double-checked
			synchronized (this) {
				value = byClass.get(singletonClass);
				if (value == null) {
					value = Reflections.newInstance(singletonClass);
					byClass.put(singletonClass, value);
				}
				return value;
			}
		}

		void put(Object implementation) {
			Class<? extends Object> clazz = implementation.getClass();
			synchronized (this) {
				Object existing = byClass.get(clazz);
				// a singleton can be registered to multiple keys, so the logic
				// of this check is correct
				if (existing != null && existing != implementation) {
					throw new IllegalStateException(Ax.format(
							"Existing registration of singleton - %s\n\t:: %s",
							clazz.getName(),
							byClass.get(clazz).getClass().getName()));
				}
				byClass.put(clazz, implementation);
			}
		}
	}

	public static <T> T newInstanceOrImpl(Class<T> clazz) {
		ClassReflector<T> classReflector = Reflections.at(clazz);
		if (classReflector.isAbstract()) {
			return impl(clazz);
		} else {
			return classReflector.newInstance();
		}
	}
}
