package cc.alcina.framework.entity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.projection.GraphProjection;

/*
 * Replacement for system configuration portion of ResourceUtilities
 *
 * TODO - clazz name -> property path segment[s] should change from
 * Class.simpleClassName to SeUtilities.getNestedSimpleName
 *
 * ... with a regression test
 *
 * FIXME ru iterate all classes, note if superfluous classref
 */
public class Configuration {
	public final static Properties properties = new Properties();

	public static String get(Class clazz, String key) {
		return properties.get(new Key(clazz, key));
	}

	public static String get(String key) {
		Class clazz = StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass();
		return get(clazz, key);
	}

	public static int getInt(Class clazz, String key) {
		return Integer.parseInt(get(clazz, key));
	}

	public static int getInt(String key) {
		Class clazz = StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass();
		return getInt(clazz, key);
	}

	public static long getLong(String key) {
		Class clazz = StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass();
		return Long.parseLong(get(clazz, key));
	}

	public static boolean has(Class clazz, String keyPart) {
		return new Key(clazz, keyPart).has();
	}

	public static boolean is(Class clazz, String key) {
		String value = get(clazz, key);
		return Boolean.valueOf(value);
	}

	public static boolean is(String key) {
		Class clazz = StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass();
		return is(clazz, key);
	}

	public static Key key(Class clazz, String keyPart) {
		return new Key(clazz, keyPart);
	}

	public static Key key(String keyPart) {
		return new Key(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), keyPart);
	}

	/*
	 *
	 */
	public static class Key {
		private Class clazz;

		private String keyPart;

		private boolean contextOverride = false;

		private String _toString;

		Key(Class clazz, String keyPart) {
			Preconditions.checkNotNull(clazz);
			this.clazz = clazz;
			this.keyPart = keyPart;
		}

		public boolean definedAndIs() {
			return has() && is();
		}

		public String get() {
			if (contextOverride) {
				String key = toString();
				if (LooseContext.has(key)) {
					return LooseContext.getString(key);
				}
			}
			return Configuration.properties.get(this);
		}

		public boolean has() {
			if (contextOverride) {
				if (LooseContext.has(toString())) {
					return true;
				}
			}
			return Configuration.properties.has(this);
		}

		public int intValue() {
			String value = get();
			return Integer.valueOf(value);
		}

		public boolean is() {
			String value = get();
			return Boolean.valueOf(value);
		}

		public long longValue() {
			String value = get();
			return Long.valueOf(value);
		}

		public Optional<Key> optional() {
			return has() ? Optional.of(this) : Optional.empty();
		}

		public void set(String value) {
			LooseContext.set(toString(), value);
		}

		@Override
		public String toString() {
			if (_toString == null) {
				_toString = Ax.format("%s.%s",
						GraphProjection.classSimpleName(clazz), keyPart);
			}
			return _toString;
		}

		public Key withContextOverride(boolean contextOverride) {
			this.contextOverride = contextOverride;
			return this;
		}
	}

	/**
	 * <p>
	 * Uses 'bundle' to denote a stringmap which is either a copy of a
	 * ResourceBundle, or another Properties resource
	 *
	 * <p>
	 * Synchronization - keyValues are CAS-d (and mutation is synchronous)
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class Properties {
		public final Topic<Void> topicInvalidated = Topic.create();

		private Map<String, PropertyValues> keyValues;

		private Map<String, PackageBundles> packageBundles = new ConcurrentHashMap<>();

		private List<PropertySet> orderedSets = new ArrayList<>();

		private Map<String, Object> immutableCustomProperties = new ConcurrentHashMap<>();

		public Properties() {
			Arrays.stream(SystemSet.values()).forEach(this::addSet);
			invalidate();
		}

		/*
		 * Security-related properties that should not be settable post-startup
		 */
		public void addImmutablePropertyKey(String key) {
			immutableCustomProperties.put(key, new Object());
		}

		public void clearCustom() {
			// TODO Auto-generated method stub
		}

		public String dump() {
			return "yup;";
		}

		// use Configuration.get() where possible
		public String get(String key) {
			PropertyValues propertyValues = keyValues.get(key);
			return propertyValues == null ? null : propertyValues.resolvedValue;
		}

		public boolean has(String key) {
			return keyValues.containsKey(key);
		}

		public Stream<String> keys() {
			return keyValues.keySet().stream();
		}

		public void loadSystemPropertiesFromConfigurationProperties() {
			keyValues.forEach((k, v) -> {
				if (k.startsWith("system.property.")) {
					k = k.substring("system.property.".length());
					System.setProperty(k, v.resolvedValue);
				}
			});
		}

		public void register(InputStream ios) {
			try {
				java.util.Properties p = new java.util.Properties();
				p.load(ios);
				ios.close();
				for (Entry<Object, Object> entry : p.entrySet()) {
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (!(key instanceof String)
							|| !(value instanceof String)) {
						continue;
					}
					set0((String) key, (String) value);
				}
				invalidate();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public void register(String propertiesString) {
			register(Io.read().string(propertiesString).asInputStream());
		}

		public String set(Class clazz, String key, String value) {
			return set(new Key(clazz, key).toString(), value);
		}

		public String set(String key, String value) {
			String prior = set0(key, value);
			invalidate();
			return prior;
		}

		/*
		 * Locking note - concurrency (of the map) means we can update at any
		 * time, synchronization enures we don't have duplicate, unneeded loads,
		 * and that invalidations are sequential
		 */
		private void ensureBundles(Key key) {
			String packageName = key.clazz.getPackageName();
			if (!packageBundles.containsKey(packageName)) {
				// double-checked
				synchronized (this) {
					if (!packageBundles.containsKey(packageName)) {
						PackageBundles bundles = new PackageBundles(key.clazz);
						packageBundles.put(packageName, bundles);
						bundles.load();
					}
				}
			}
		}

		private synchronized void invalidate() {
			Set<String> keys = new LinkedHashSet<>();
			orderedSets.forEach(set -> set.populateKeys(keys));
			Map<String, PropertyValues> keyValues = keys.stream().collect(
					AlcinaCollectors.toLinkedHashMap(Function.identity(), k -> {
						PropertyValues propertyValues = new PropertyValues(k);
						propertyValues.resolve();
						return propertyValues;
					}));
			// swap
			this.keyValues = keyValues;
			topicInvalidated.signal();
		}

		private String set0(String key, String value) {
			Preconditions
					.checkState(!immutableCustomProperties.containsKey(key));
			return getSet(SystemSet.custom).get().put(key, value);
		}

		void addSet(SystemSet systemSet) {
			Preconditions.checkArgument(getSet(systemSet).isEmpty());
			PropertySet set = new PropertySet(systemSet);
			orderedSets.add(set);
		}

		void dump(boolean flat) {
			Map<String, PropertyValues> toDump = keyValues;
			Map<String, String> map = toDump.keySet().stream().sorted()
					.collect(AlcinaCollectors.toLinkedHashMap(k -> k,
							k -> toDump.get(k).resolvedValue));
			Ax.out(map.entrySet());
		}

		String get(Key key) {
			String stringKey = key.toString();
			ensureBundles(key);
			PropertyValues propertyValues = keyValues.get(stringKey);
			if (propertyValues != null) {
				return propertyValues.resolvedValue;
			} else {
				if (key.clazz != null) {
					Class superclass = key.clazz.getSuperclass();
					if (superclass != null && superclass != Object.class) {
						return get(new Key(superclass, key.keyPart)
								.withContextOverride(key.contextOverride));
					}
				}
				return null;
			}
		}

		Optional<PropertySet> getSet(Object key) {
			String setKey = key.toString();
			return orderedSets.stream().filter(s -> s.key.equals(setKey))
					.findFirst();
		}

		boolean has(Key key) {
			return false;
		}

		class PackageBundles {
			String packageName;

			Map<PropertySet, StringMap> bundles = new LinkedHashMap<>();

			StringMap unionMap = new StringMap();

			private ClassLoader classLoader;

			public PackageBundles(Class clazz) {
				packageName = clazz.getPackageName();
				classLoader = clazz.getClassLoader();
			}

			public boolean containsKey(String key) {
				return unionMap.containsKey(key);
			}

			public String getValue(String key) {
				return unionMap.get(key);
			}

			public Set<String> keys() {
				return unionMap.keySet();
			}

			public void load() {
				orderedSets.stream().filter(PropertySet::usesBundles)
						.forEach(set -> {
							String specifier = set.specifier();
							// FIXME - ru - Bundle -> Configuration
							String base = Ax.format("%s.%s%s", packageName,
									"Bundle", specifier);
							try {
								ResourceBundle bundle = ResourceBundle
										.getBundle(base, Locale.getDefault(),
												classLoader);
								StringMap map = new StringMap();
								bundle.keySet().forEach(key -> {
									map.put(key, bundle.getString(key));
								});
								bundles.put(set, map);
								unionMap.putAll(map);
							} catch (MissingResourceException e) {
								if (set.isRequired()) {
									throw e;
								}
							}
						});
				invalidate();
			}

			@Override
			public String toString() {
				return bundles.toString();
			}
		}

		class PropertySet {
			private String key;

			private SystemSet systemSet;

			StringMap map;

			public PropertySet(SystemSet set) {
				this.key = set.name();
				this.systemSet = set;
				if (set.usesBundles()) {
				} else {
					map = new StringMap();
				}
			}

			PropertySet(String key) {
				this.key = key;
				// non-system-set sets do not resolve (they're compacted
				// (package bundles) onto the base systemset)
			}

			public boolean isRequired() {
				return systemSet == SystemSet.base;
			}

			public String put(String key, String value) {
				StringMap clone = map.clone();
				String prior = clone.put(key, value);
				map = clone;
				return prior;
			}

			public Stream<ValueSource> resolve(String key) {
				if (!resolves()) {
					return Stream.empty();
				}
				Stream<ValueSource> result = null;
				if (usesBundles()) {
					result = ValueSource.fromBundles(this, key, packageBundles);
				} else {
					result = Stream.of(ValueSource.fromMap(this, key, map));
				}
				return result.filter(Objects::nonNull);
			}

			public String specifier() {
				return systemSet == SystemSet.base ? "" : "_" + key;
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.format("PropertySet: %s", key);
				format.appendIfNotBlankKv("Map", map);
				return format.toString();
			}

			private boolean resolves() {
				return systemSet != null;
			}

			void populateKeys(Set<String> keys) {
				if (!resolves()) {
					return;
				}
				if (usesBundles()) {
					packageBundles.values()
							.forEach(bundle -> keys.addAll(bundle.keys()));
				} else {
					keys.addAll(map.keySet());
				}
			}

			boolean usesBundles() {
				return map == null;
			}
		}

		class PropertyValues {
			String resolvedValue;

			List<ValueSource> sources;

			private String key;

			public PropertyValues(String key) {
				this.key = key;
			}

			public void resolve() {
				sources = orderedSets.stream().flatMap(set -> set.resolve(key))
						.collect(Collectors.toList());
				resolvedValue = sources.isEmpty() ? null
						: Ax.last(sources).value;
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.appendKeyValues(key, resolvedValue);
				format.indent(2);
				sources.forEach(format::line);
				return format.toString();
			}
		}

		enum SystemSet {
			set_loader, base, custom;

			boolean usesBundles() {
				return this == base;
			}
		}

		static class ValueSource {
			static Stream<ValueSource> fromBundles(PropertySet propertySet,
					String key, Map<String, PackageBundles> packageBundles) {
				List<ValueSource> results = packageBundles.values().stream()
						.filter(bundles -> bundles.containsKey(key))
						.map(bundles -> new ValueSource(bundles, propertySet,
								bundles.getValue(key)))
						.collect(Collectors.toList());
				if (results.size() > 1) {
					throw new IllegalStateException(Ax.format(
							"Incorrect configuration - multiple matches for '%s' - \n%s",
							key, results));
				}
				return Stream.of(Ax.first(results));
			}

			static ValueSource fromMap(PropertySet propertySet, String key,
					StringMap map) {
				return map.containsKey(key)
						? new ValueSource(null, propertySet, map.get(key))
						: null;
			}

			PackageBundles packageBundles;

			PropertySet set;

			String value;

			ValueSource(PackageBundles packageBundles, PropertySet set,
					String value) {
				this.packageBundles = packageBundles;
				this.set = set;
				this.value = value;
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.appendKeyValues(set.key, value);
				if (packageBundles != null) {
					format.indent(2);
					format.append(packageBundles);
				}
				return format.toString();
			}
		}
	}
}
