package cc.alcina.framework.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.Ax;
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
 */
public class Configuration {
	private final static Properties properties = new Properties();

	public static String get(Class clazz, String key) {
		return properties.get(new Key(clazz, key));
	}

	public static String get(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return value;
	}

	public static int getInt(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return Integer.parseInt(value);
	}

	public static long getLong(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return Long.parseLong(value);
	}

	public static boolean has(Class clazz, String keyPart) {
		String key = clazz.getSimpleName() + "." + keyPart;
		return ResourceUtilities.isDefined(key);
	}

	public static boolean is(Class clazz, String key) {
		String value = get(clazz, key);
		return Boolean.valueOf(value);
	}

	public static boolean is(String key) {
		String value = get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), key);
		return Boolean.valueOf(value);
	}

	public static Key key(Class clazz, String keyPart) {
		return new Key(clazz, keyPart);
	}

	public static Key key(String keyPart) {
		return new Key(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), keyPart);
	}

	public static Properties properties() {
		return properties;
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
	 * Uses 'bundle' to denote a stringmap which is either a copy of a
	 * ResourceBundle, or another Properties resource
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class Properties {
		static Logger logger = LoggerFactory.getLogger(ResourceUtilities.class);

		public final Topic<Void> propertiesInvalidated = Topic.create();

		private Map<String, PropertyValues> keyValues = new ConcurrentHashMap<>();

		private Map<String, PackageBundles> packageBundles = new ConcurrentHashMap<>();

		private List<PropertySet> orderedSets = new ArrayList<>();

		private Map<String, Object> immutableCustomProperties = new ConcurrentHashMap<>();

		public Properties() {
			Arrays.stream(SystemSet.values()).forEach(this::addSet);
		}

		/*
		 * Security-related properties that should not be settable post-startup
		 */
		public void addImmutablePropertyKey(String key) {
			immutableCustomProperties.put(key, new Object());
		}

		public String dump() {
			return "yup;";
		}

		// use Configuration.get() where possible
		public String get(String key) {
			PropertyValues propertyValues = keyValues.get(key);
			return propertyValues == null ? null : propertyValues.resolvedValue;
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

		public String set(String key, String value) {
			Preconditions
					.checkState(!immutableCustomProperties.containsKey(key));
			String prior = getSet(SystemSet.custom).get().put(key, value);
			invalidate();
			return prior;
		}

		private void ensureBundles(Key key) {
			String packageName = key.clazz.getPackageName();
			// compute if absent both synchronizes creation, and populates
			// cache
			packageBundles.computeIfAbsent(packageName, name -> {
				PackageBundles bundles = new PackageBundles(key.clazz);
				packageBundles.put(packageName, bundles);
				bundles.load();
				return bundles;
			});
		}

		private void invalidate() {
			// TODO Auto-generated method stub
		}

		void addSet(SystemSet systemSet) {
			Preconditions.checkArgument(getSet(systemSet).isEmpty());
			PropertySet set = new PropertySet(systemSet);
			orderedSets.add(set);
		}

		String get(Key key) {
			String stringKey = key.toString();
			ensureBundles(key);
			if (keyValues.containsKey(stringKey)) {
				return keyValues.get(stringKey).resolvedValue;
			} else {
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

			private ClassLoader classLoader;

			public PackageBundles(Class clazz) {
				packageName = clazz.getPackageName();
				classLoader = clazz.getClassLoader();
			}

			public void load() {
				Set<String> keys = new LinkedHashSet<>();
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
								keys.addAll(map.keySet());
							} catch (MissingResourceException e) {
								if (set.isRequired()) {
									throw e;
								}
							}
						});
				keys.forEach(key -> {
					PropertyValues values = new PropertyValues(key);
					values.resolve();
					keyValues.put(key, values);
				});
				invalidate();
			}
		}

		class PropertySet {
			private String key;

			private SystemSet set;

			StringMap map;

			public PropertySet(SystemSet set) {
				this(set.name());
				this.set = set;
			}

			PropertySet(String key) {
				this.key = key;
			}

			public boolean isRequired() {
				return set == SystemSet.base;
			}

			public String put(String key, String value) {
				StringMap clone = map.clone();
				String prior = clone.put(key, value);
				map = clone;
				return prior;
			}

			public Stream<ValueSource> resolve(String key) {
				Stream<ValueSource> result = null;
				if (usesBundles()) {
				} else {
					result = Stream.of(ValueSource.fromMap(this, key, map));
				}
				return result.filter(Objects::nonNull);
			}

			public String specifier() {
				return set == SystemSet.base ? "" : "_" + key;
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
				List<ValueSource> sources = orderedSets.stream()
						.flatMap(set -> set.resolve(key))
						.collect(Collectors.toList());
				resolvedValue = sources.isEmpty() ? null
						: Ax.last(sources).value;
			}
		}

		enum SystemSet {
			set_loader, base, custom;
		}

		static class ValueSource {
			public static ValueSource fromMap(PropertySet propertySet,
					String key, StringMap map) {
				return map.containsKey(key)
						? new ValueSource(null, propertySet, map.get(key))
						: null;
			}

			PackageBundles packageBundles;

			PropertySet set;

			String value;

			public ValueSource(PackageBundles packageBundles, PropertySet set,
					String value) {
				this.packageBundles = packageBundles;
				this.set = set;
				this.value = value;
			}
		}
	}
}
