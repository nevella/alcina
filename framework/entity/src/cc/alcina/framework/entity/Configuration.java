package cc.alcina.framework.entity;

import java.io.File;
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

		private String _toString;

		private Key(Class clazz, String keyPart, boolean allowNullClass) {
			Preconditions.checkState(clazz != null || allowNullClass);
			this.clazz = clazz;
			this.keyPart = keyPart;
			if (clazz == null) {
				_toString = keyPart;
			}
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
	 * ResourceBundle, or another java.util.Properties resource
	 *
	 * <p>
	 * Synchronization - a fast path checks for non-nulls in keyValues, any
	 * mutations to keyValues (and thus packageBundles) are synchronized on the
	 * Properties instance
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class Properties {
		public final Topic<Void> topicInvalidated = Topic.create();

		private Map<String, PropertyValues> keyValues = new ConcurrentHashMap();

		// access is synchronized
		private Map<String, PackageBundles> packageBundles = new LinkedHashMap<>();

		// mutation is startup-only
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

		// FIXME - ru - to private near proj completion
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

		private void ensureBundles(Key key) {
			String packageName = key.clazz.getPackageName();
			if (!packageBundles.containsKey(packageName)) {
				PackageBundles bundles = new PackageBundles(key.clazz);
				packageBundles.put(packageName, bundles);
				bundles.load();
			}
		}

		private synchronized void invalidate() {
			Set<String> keys = new LinkedHashSet<>();
			orderedSets.forEach(set -> set.populateKeys(keys));
			keys.stream().map(Key::stringKey).forEach(this::ensureValues);
			topicInvalidated.signal();
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
		}

		void dump(boolean flat) {
			Map<String, String> map = keyValues.keySet().stream().sorted()
					.collect(AlcinaCollectors.toLinkedHashMap(k -> k,
							k -> keyValues.get(k).resolvedValue));
			Ax.out(map.entrySet());
		}

		PropertyValues ensureValues(Key key) {
			String stringKey = key.toString();
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
						propertyValues = keyValues.get(cursor.toString());
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
						if (cursor == null
								|| clazz.getPackageName().startsWith("java")) {
							break;
						}
						cursor = new Key(clazz, cursor.keyPart)
								.withContextOverride(cursor.contextOverride);
					}
					// unresolved, PropertyValues.resolvedValue==null
					propertyValues = new PropertyValues(stringKey);
					keyValues.put(cursor.toString(), propertyValues);
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
						this, childSupplier, false);
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
