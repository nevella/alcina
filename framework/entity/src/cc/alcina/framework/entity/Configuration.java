package cc.alcina.framework.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.entity.Configuration.PropertyTree.PropertyNode;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.CsvCols;
import cc.alcina.framework.entity.util.CsvCols.CsvRow;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;

/**
 * <h2>Structured application configuration</h2>
 * <p>
 * Application configuration is a key-value structure, where the keys are either
 * [class-simple-name].[subkey] or specially handled fully qualified directives
 * (for configuration that doesn't nicely map to classes, such as log levels or
 * jvm system properties).
 *
 * <p>
 * Default configuration values are in the package configuration.properties
 * files, applications specify a list of overriding <i>configuration sets</i>
 * which modify the defaults.
 *
 * <h3>Usage</h3> <code><pre>

[confguration.properties]

Foo.enabled=true
Foo.name=some-froo

[access in code]

// (called from code within Foo)
boolean enabled = Configuration.is("enabled");
// enabled == true, class parameter defaults to caller class (which is Foo)
String name = Configuration.get("name");
// name == "some-froo"

(called outside Foo)
boolean enabled = Configuration.is(Foo.class,"enabled");
//enabled == true


 * </pre></code>
 */
public class Configuration {
	public final static Properties properties = new Properties();

	/*
	 * Limited pre-stack-walker support
	 */
	public static boolean useStackTraceCallingClass;

	public static String get(Class clazz, String key) {
		return properties.get(new Key(clazz, key));
	}

	public static String get(String key) {
		Class clazz = null;
		if (useStackTraceCallingClass) {
			clazz = getStacktraceCallingClass();
		} else {
			clazz = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
		}
		return get(clazz, key);
	}

	public static int getInt(Class clazz, String key) {
		return Integer.parseInt(get(clazz, key));
	}

	public static int getInt(String key) {
		Class clazz = null;
		if (useStackTraceCallingClass) {
			clazz = getStacktraceCallingClass();
		} else {
			clazz = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
		}
		return getInt(clazz, key);
	}

	public static long getLong(String key) {
		Class clazz = null;
		if (useStackTraceCallingClass) {
			clazz = getStacktraceCallingClass();
		} else {
			clazz = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
		}
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
		Class clazz = null;
		if (useStackTraceCallingClass) {
			clazz = getStacktraceCallingClass();
		} else {
			clazz = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
		}
		return is(clazz, key);
	}

	public static Key key(Class clazz, String keyPart) {
		return new Key(clazz, keyPart);
	}

	public static Key key(String keyPart) {
		Class clazz = null;
		if (useStackTraceCallingClass) {
			clazz = getStacktraceCallingClass();
		} else {
			clazz = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
		}
		return new Key(clazz, keyPart);
	}

	static Class getStacktraceCallingClass() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		StackTraceElement caller = stackTrace[2];
		return Reflections.forName(caller.getClassName());
	}

	public static class ConfigurationFile {
		private String path;

		private String set;

		private String packageName;

		private String url;

		String contents;

		public ConfigurationFile() {
		}

		public ConfigurationFile(String base, File file, String set) {
			this.path = file.getPath();
			this.set = set;
			if (base != null) {
				this.packageName = file.getParentFile().getPath()
						.substring(base.length()).replace("/", ".")
						.replaceFirst("^\\.", "");
			}
			try {
				this.url = file.toURI().toURL().toString();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			load();
		}

		public String getPackageName() {
			return this.packageName;
		}

		public String getPath() {
			return this.path;
		}

		public String getSet() {
			return this.set;
		}

		public boolean provideContainsNonNamespaced() {
			List<String> keys = StringMap.fromPropertyString(contents).keySet()
					.stream().filter(k -> !k.contains("."))
					.collect(Collectors.toList());
			return keys.size() > 0;
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public void setSet(String set) {
			this.set = set;
		}

		@Override
		public String toString() {
			return Ax.format("%s -\n\t%s", packageName, path);
		}

		private void load() {
			contents = Io.read().url(url).asString();
		}
	}

	/*
	 *
	 */
	public static class Key {
		// public for tooling, can remove once migration
		// (TaskRefactorConfigSets) complete
		public static Key stringKey(String key) {
			return new Key(null, key, true);
		}

		private Class clazz;

		private String keyPart;

		private boolean contextOverride = false;

		String stringRepresentation;

		private Key(Class clazz, String keyPart, boolean allowNullClass) {
			Preconditions.checkState(clazz != null || allowNullClass);
			this.clazz = clazz;
			this.keyPart = keyPart;
			stringRepresentation = clazz == null ? keyPart
					: Ax.format("%s.%s", GraphProjection.classSimpleName(clazz),
							keyPart);
		}

		Key(Class clazz, String keyPart) {
			this(clazz, keyPart, false);
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

		public Key required() {
			Ax.checkNotBlank(get());
			return this;
		}

		public void set(String value) {
			LooseContext.set(toString(), value);
		}

		@Override
		public String toString() {
			return stringRepresentation;
		}

		public Key withContextOverride(boolean contextOverride) {
			this.contextOverride = contextOverride;
			return this;
		}
	}

	/**
	 * <p>
	 * Uses 'bundle' to denote a stringmap which is either a copy of a
	 * ResourceBundle, or another java.util.Properties resource
	 *
	 * <p>
	 * Synchronization - a fast path checks for non-nulls in keyValues, any
	 * mutations to keyValues (and thus packageBundles) are synchronized on the
	 * Properties instance
	 *
	 *
	 *
	 */
	public static class Properties {
		public final Topic<Void> topicInvalidated = Topic.create();

		private ClassLoader classLoader;

		private boolean useSets;

		private Map<String, PropertyValues> keyValues = new ConcurrentHashMap();

		// access is synchronized
		private Map<String, PackageBundle> packageBundles = new LinkedHashMap<>();

		PropertySet base;

		// mutation is startup-only
		private List<PropertySet> orderedSets = new ArrayList<>();

		private Map<String, Object> immutableCustomProperties = new ConcurrentHashMap<>();

		Pattern includePattern = Pattern
				.compile("include\\.(resource|file)=(.+)");

		boolean logResourceLoad;

		public Properties() {
			Arrays.stream(SystemSet.values()).forEach(this::addSet);
			logResourceLoad = Boolean.getBoolean(
					"cc.alcina.framework.entity.Configuration.Properties.logResourceLoad");
			invalidate();
		}

		/*
		 * Security-related properties that should not be settable post-startup
		 */
		public void addImmutablePropertyKey(String key) {
			immutableCustomProperties.put(key, new Object());
		}

		public String asString(boolean flat) {
			Map<String, String> map = keyValues.keySet().stream().sorted()
					.collect(AlcinaCollectors.toLinkedHashMap(k -> k,
							k -> keyValues.get(k).resolvedValue));
			return map.entrySet().stream().map(Object::toString)
					.collect(Collectors.joining("\n"));
		}

		public void clearCustom() {
			// TODO Auto-generated method stub
		}

		public void dump() {
			Ax.out(asString(true));
		}

		// use Configuration.get() where possible
		public String get(String key) {
			PropertyValues propertyValues = keyValues.get(key);
			return propertyValues == null ? null : propertyValues.resolvedValue;
		}

		public ClassLoader getClassLoader() {
			return this.classLoader;
		}

		public boolean has(String key) {
			return keyValues.containsKey(key);
		}

		public boolean isUseSets() {
			return this.useSets;
		}

		public Stream<String> keys() {
			return keyValues.keySet().stream();
		}

		public synchronized void load(Runnable runnable) {
			keyValues.clear();
			orderedSets.forEach(PropertySet::clear);
			runnable.run();
		}

		public void loadSystemPropertiesFromConfigurationProperties() {
			keyValues.forEach((k, v) -> {
				if (k.startsWith("system.property.")) {
					k = k.substring("system.property.".length());
					System.setProperty(k, v.resolvedValue);
				}
			});
		}

		public void register(String propertiesString) {
			register0(propertiesString);
			invalidate();
		}

		public String set(Class clazz, String key, String value) {
			return set(new Key(clazz, key).stringRepresentation, value);
		}

		public String set(String key, String value) {
			String prior = set0(key, value);
			invalidate();
			return prior;
		}

		public void setClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		public void setUseSets(boolean useSets) {
			this.useSets = useSets;
		}

		private void ensureBundles(Key key) {
			String packageName = key.clazz.getPackage().getName();
			if (!packageBundles.containsKey(packageName)) {
				PackageBundle bundles = new PackageBundle(key.clazz);
				packageBundles.put(packageName, bundles);
				bundles.load();
			}
		}

		private synchronized void invalidate() {
			Set<String> keys = new LinkedHashSet<>();
			orderedSets.forEach(set -> set.populateKeys(keys));
			keyValues.clear();
			keys.stream().map(Key::stringKey).forEach(this::ensureValues);
			topicInvalidated.signal();
		}

		private boolean processInclude(String line) {
			Matcher matcher = includePattern.matcher(line);
			if (matcher.matches()) {
				String type = matcher.group(1);
				String path = matcher.group(2);
				String contents = null;
				switch (type) {
				case "resource": {
					// trim leading slash, required for
					// classloader.getResourceAsStream() (but not
					// class.getResourceAsStream
					//
					// trace with jvm property
					// -Dcc.alcina.framework.entity.Configuration.Properties.logResourceLoad=true
					String trimmedPath = path.substring(1);
					if (logResourceLoad) {
						Ax.out("Loading config path: %s", trimmedPath);
					}
					contents = Io.read()
							.fromStream(provideClassLoader()
									.getResourceAsStream(trimmedPath))
							.asString();
					break;
				}
				case "file": {
					contents = Io.read().path(path).asString();
					break;
				}
				default:
					throw new UnsupportedOperationException();
				}
				register0(contents);
				return true;
			} else {
				return false;
			}
		}

		private ClassLoader provideClassLoader() {
			return classLoader != null ? classLoader
					: getClass().getClassLoader();
		}

		private void register0(String propertiesString) {
			FormatBuilder nonIncludeBuilder = new FormatBuilder();
			// expand includes
			Arrays.stream(propertiesString.split("\n")).forEach(line -> {
				if (processInclude(line)) {
					//
				} else {
					nonIncludeBuilder.line(line);
				}
			});
			// register non-includes
			StringMap map = StringMap
					.fromPropertyString(nonIncludeBuilder.toString());
			map.forEach((key, value) -> {
				set0(key, value);
			});
		}

		// threadsafe - the put is a copy-on-write
		private String set0(String key, String value) {
			Preconditions
					.checkState(!immutableCustomProperties.containsKey(key));
			return getSet(SystemSet.custom).get().put(key, value);
		}

		void addSet(SystemSet systemSet) {
			Preconditions.checkArgument(getSet(systemSet).isEmpty());
			PropertySet set = new PropertySet(systemSet);
			orderedSets.add(set);
			if (systemSet == SystemSet.base) {
				base = set;
			}
		}

		PropertyValues ensureValues(Key key) {
			String stringKey = key.stringRepresentation;
			PropertyValues propertyValues = keyValues.get(stringKey);
			if (propertyValues != null) {
				return propertyValues;
			} else {
				// could be optimisation miss, check
				synchronized (this) {
					propertyValues = keyValues.get(stringKey);
					if (propertyValues != null) {
						return propertyValues;
					}
					Key cursor = key;
					while (true) {
						Class clazz = cursor.clazz;
						if (clazz != null) {
							ensureBundles(cursor);
						}
						propertyValues = keyValues
								.get(cursor.stringRepresentation);
						if (propertyValues != null) {
							if (cursor != key) {
								keyValues.put(stringKey,
										propertyValues.copyFor(stringKey));
							}
							return propertyValues;
						}
						if (clazz == null) {
							break;
						}
						clazz = clazz.getSuperclass();
						if (clazz == null || clazz.getPackage().getName()
								.startsWith("java")) {
							break;
						}
						cursor = new Key(clazz, cursor.keyPart)
								.withContextOverride(cursor.contextOverride);
					}
					// unresolved, PropertyValues.resolvedValue==null
					propertyValues = new PropertyValues(stringKey);
					keyValues.put(cursor.stringRepresentation, propertyValues);
					return propertyValues;
				}
			}
		}

		String get(Key key) {
			return ensureValues(key).resolvedValue;
		}

		Optional<PropertySet> getSet(Object key) {
			String setKey = key.toString();
			return orderedSets.stream().filter(s -> s.key.equals(setKey))
					.findFirst();
		}

		boolean has(Key key) {
			return ensureValues(key).exists();
		}

		class PackageBundle {
			String packageName;

			StringMap map = new StringMap();

			private ClassLoader classLoader;

			public PackageBundle(Class clazz) {
				packageName = clazz.getPackage().getName();
				classLoader = clazz.getClassLoader();
			}

			public boolean containsKey(String key) {
				return map.containsKey(key);
			}

			public String getValue(String key) {
				return map.get(key);
			}

			public Set<String> keys() {
				return map.keySet();
			}

			public void load() {
				String baseName = useSets ? "configuration" : "Bundle";
				String bundleBase = Ax.format("%s.%s", packageName, baseName);
				ResourceBundle bundle = ResourceBundle.getBundle(bundleBase,
						Locale.getDefault(), classLoader);
				bundle.keySet().forEach(key -> {
					map.put(key, bundle.getString(key));
				});
				invalidate();
			}

			@Override
			public String toString() {
				return map.toString();
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

			void clear() {
				if (map != null) {
					map.clear();
				}
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

			String registeredAt;

			public PropertyValues(String key) {
				this.key = key;
				resolve();
			}

			public PropertyValues copyFor(String stringKey) {
				PropertyValues result = new PropertyValues(key);
				result.registeredAt = stringKey;
				return result;
			}

			public boolean exists() {
				return resolvedValue != null;
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
			base, custom;

			boolean usesBundles() {
				return this == base;
			}
		}

		static class ValueSource {
			static Stream<ValueSource> fromBundles(PropertySet propertySet,
					String key, Map<String, PackageBundle> packageBundles) {
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

			PackageBundle packageBundles;

			PropertySet set;

			String value;

			ValueSource(PackageBundle packageBundles, PropertySet set,
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

	public static class PropertyTree extends Tree<PropertyNode> {
		public PropertyTree() {
			PropertyNode root = new PropertyNode(null, "", "");
			setRoot(root);
		}

		public void add(ConfigurationFile file) {
			StringMap map = StringMap.fromPropertyString(file.contents);
			if (Ax.notBlank(file.packageName)) {
				PropertyNode packageNode = ensurePackageNode(file.packageName);
				packageNode.addValues(file, map);
			} else {
				// if matching an existing class.property key, add, otherwise
				// add to the root package
				List<PropertyNode> packageNodes = getRoot().depthFirst();
				Map<String, PropertyNode> byKey = packageNodes.stream()
						.collect(AlcinaCollectors.toKeyMap(n -> n.key));
				map.forEach((key, value) -> {
					if (byKey.containsKey(key)) {
						byKey.get(key).addValue(file, value);
					} else {
						getRoot().ensureKeyChild(key).addValue(file, value);
					}
				});
			}
		}

		public List<String> allKeys() {
			List<PropertyNode> nodes = getRoot().depthFirst();
			nodes.stream().collect(Collectors.toList())
					.forEach(PropertyNode::sortChildren);
			return nodes.stream().map(node -> node.key).filter(Ax::notBlank)
					.map(Object::toString).distinct().sorted()
					.collect(Collectors.toList());
		}

		public String asCsv() {
			List<PropertyNode> nodes = getRoot().depthFirst();
			nodes.stream().collect(Collectors.toList())
					.forEach(PropertyNode::sortChildren);
			CsvCols cols = new CsvCols("");
			nodes = getRoot().depthFirst();
			Stream.of(Header.values()).forEach(cols::addColumn);
			nodes.stream().forEach(node -> node.addTo(cols));
			return cols.toCsv();
		}

		public StringMap asMap() {
			List<PropertyNode> nodes = getRoot().depthFirst();
			nodes.stream().collect(Collectors.toList())
					.forEach(PropertyNode::sortChildren);
			StringMap result = new StringMap();
			nodes.stream().filter(n -> Ax.notBlank(n.key))
					.forEach(n -> result.put(n.key, n.lastValue()));
			return result;
		}

		public void removeKeys(Set<String> remove) {
			List<PropertyNode> nodes = getRoot().depthFirst();
			nodes.forEach(n -> n.removeKeys(remove));
			nodes.stream().filter(PropertyNode::canRemove)
					.forEach(PropertyNode::removeFromParent);
		}

		private PropertyNode ensurePackageNode(String packageName) {
			PropertyNode cursor = getRoot();
			List<String> list = Arrays.stream(packageName.split("\\."))
					.filter(Ax::notBlank).collect(Collectors.toList());
			for (String segment : list) {
				PropertyNode child = cursor.getPackageSegmentChild(segment);
				if (child != null) {
					cursor = child;
				} else {
					child = new PropertyNode(cursor, segment, null);
					cursor.getChildren().add(child);
					cursor = child;
				}
			}
			return cursor;
		}

		public class PropertyNode extends Tree.TreeNode<PropertyNode>
				implements Comparable<PropertyNode> {
			String packageSegment;

			String key;

			List<FileValue> values = new ArrayList<>();

			private PropertyNode parent;

			public PropertyNode(PropertyNode parent, String packageSegment,
					String key) {
				this.parent = parent;
				this.packageSegment = packageSegment;
				this.key = key;
			}

			public void addValues(ConfigurationFile file, StringMap map) {
				map.forEach((k, v) -> ensureKeyChild(k).addValue(file, v));
			}

			@Override
			public int compareTo(PropertyNode o) {
				{
					int cmp = CommonUtils.compareWithNullMinusOne(
							packageSegment, o.packageSegment);
					if (cmp != 0) {
						return cmp;
					}
				}
				{
					int cmp = CommonUtils.compareWithNullMinusOne(key, o.key);
					return cmp;
				}
			}

			public void removeKeys(Set<String> remove) {
				if (Ax.notBlank(key) && remove.contains(key)) {
					values.clear();
				}
			}

			@Override
			public String toString() {
				return Ax.notBlank(packageSegment) ? packageSegment : key;
			}

			private PropertyNode ensureKeyChild(String key) {
				PropertyNode child = getKeyChild(key);
				if (child != null) {
					return child;
				} else {
					child = new PropertyNode(this, null, key);
					getChildren().add(child);
					return child;
				}
			}

			private String packageName() {
				PropertyNode cursor = this;
				List<String> segments = new ArrayList<>();
				while (cursor != null) {
					segments.add(0, cursor.packageSegment);
					cursor = cursor.parent;
				}
				return segments.stream().filter(Ax::notBlank)
						.collect(Collectors.joining("."));
			}

			void addTo(CsvCols cols) {
				CsvRow row = cols.addRow();
				if (key == null) {
					row.set(Header.Package, packageName());
				} else {
					row.set(Header.Key, key);
					int idx = 0;
					for (FileValue value : values) {
						if (idx++ > 0) {
							row = cols.addRow();
						}
						row.set(Header.File, value.file.path);
						row.set(Header.Value, value.value);
						row.set(Header.InputSet, value.file.set);
					}
				}
			}

			void addValue(ConfigurationFile file, String value) {
				values.add(new FileValue(file, value));
			}

			boolean canRemove() {
				return values.isEmpty() && getChildren().isEmpty();
			}

			List<PropertyNode> depthFirst() {
				Function<PropertyNode, List<PropertyNode>> childSupplier = n -> (List) n
						.getChildren();
				DepthFirstTraversal<PropertyNode> traversal = new DepthFirstTraversal<PropertyNode>(
						this, childSupplier);
				return traversal.stream().collect(Collectors.toList());
			}

			PropertyNode getKeyChild(String key) {
				return getChildren().stream().map(n -> (PropertyNode) n)
						.filter(c -> Objects.equals(c.key, key)).findFirst()
						.orElse(null);
			}

			PropertyNode getPackageSegmentChild(String segment) {
				return getChildren().stream().map(n -> (PropertyNode) n)
						.filter(c -> Objects.equals(c.packageSegment, segment))
						.findFirst().orElse(null);
			}

			String lastValue() {
				return Ax.last(values).value;
			}

			void removeFromParent() {
				parent.getChildren().remove(this);
			}
		}

		static class FileValue {
			ConfigurationFile file;

			String value;

			FileValue(ConfigurationFile file, String value) {
				this.file = file;
				this.value = value;
			}
		}

		enum Header {
			Package, Key, File, Value, Comment, InputSet, OutputSet;
		}
	}
}
