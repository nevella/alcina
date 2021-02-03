package cc.alcina.framework.common.client.serializer.flat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasElementType;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.util.TextUtils;

/**
 * <p>
 * This class serializes directed graphs as a flat k/v list. Annotations on the
 * serialized classes provide naming information to compress (and make more
 * human-friendly) the key names. The notional algorithm is:
 * </p>
 * <ul>
 * <li>Generate the non-compressed list, producing a list of path (parallel with
 * variations to jsonptr)/value pairs
 * <li>Remove where identical to default
 * <li>Compress using class/field annotations:
 * <ul>
 * <li>Remove path segment if default
 * <li>Replace path segment with short name
 * <li>Replace index with (compressed) class name/ordinal tuple if member of
 * polymporhpic collection
 * </ul>
 * </ul>
 * <p>
 * Polymporhpic collections must define the complete list of allowable members,
 * to ensure serialization uniqueness
 * </p>
 * <p>
 * Constraints: Collections cannot contain nulls, and all-default value
 * TreeSerializable elements will not be serialized
 * </p>
 *
 * <h2>TODO:</h2>
 * <ul>
 * <li>Build resolution map at earch path node (single-prop top-level doesn't
 * elide default btw) - e.g. ServerTask.value
 * <li>Use for ser/deser
 * <li>deser
 * <li>annotation builders (bindableSDef)
 * <li>annotation builders (publication)
 * <li>transformmanager serialization
 * <li>pluggable client sdef ser/deser
 * <li>serialize with 2 copies of each collection<treeser> elt; all enum values;
 * hash string; check roundtrip gives identical kryo bytes
 * <li>(later) GWT RPC
 * <li>Apps
 * </ul>
 *
 * @author nick@alcina.cc
 */
public class FlatTreeSerializer {
	private static final String CLASS = "#class";

	private static String NULL_MARKER = "__fts_NULL__";

	private static Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<Class, Map<String, Property>> deSerializationClassAliasProperty = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<Property, Map<String, Class>> deSerializationPropertyAliasClass = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	public static <T> T deserialize(Class<T> clazz, String value) {
		Options options = new Options().withShortPaths(true).withDefaults(true);
		return deserialize(clazz, value, options);
	}

	public static <T> T deserialize(Class<T> clazz, String value,
			Options options) {
		if (value == null) {
			return null;
		}
		State state = new State();
		state.options = options;
		state.keyValues = StringMap.fromPropertyString(value);
		if (clazz == null) {
			clazz = Reflections.classLookup()
					.getClassForName(state.keyValues.get(CLASS));
		}
		Node node = new Node(null, Reflections.newInstance(clazz), null);
		new FlatTreeSerializer(state).deserialize(node);
		return (T) node.value;
	}

	public static <T> T deserialize(String value) {
		Options options = new Options().withShortPaths(true).withDefaults(true)
				.withTopLevelTypeInfo(true);
		return deserialize(null, value, options);
	}

	public static String serialize(Object object) {
		return serialize(object,
				new Options().withTopLevelTypeInfo(true).withShortPaths(true));
	}

	public static String serialize(Object object, Options options) {
		if (object == null) {
			return null;
		}
		State state = new State();
		state.options = options;
		Node node = new Node(null, object, Reflections.classLookup()
				.getTemplateInstance(object.getClass()));
		state.pending.add(node);
		FlatTreeSerializer serializer = new FlatTreeSerializer(state);
		serializer.serialize();
		return state.keyValues.sorted().toPropertyString();
	}

	private static boolean isLeafValue(Object value) {
		if (value == null) {
			return true;
		}
		Class<? extends Object> clazz = value.getClass();
		if (CommonUtils.stdAndPrimitives.contains(clazz)) {
			return true;
		}
		if (clazz.isEnum() || CommonUtils.isEnumSubclass(clazz)) {
			return true;
		}
		return false;
	}

	State state;

	private FlatTreeSerializer(State state) {
		this.state = state;
	}

	private void deserialize(Node root) {
		state.keyValues.forEach((path, value) -> {
			Node cursor = root;
			String[] segments = path.split("\\.");
			boolean resolvingLastSegment = false;
			for (int idx = 0; idx < segments.length; idx++) {
				String segment = segments[idx];
				String segmentPath = segment;
				int index = 1;
				if (segment.matches("(.+?)(\\d+)")) {
					segmentPath = segment.replaceFirst("(.+?)(\\d+)", "$1");
					index = Integer.parseInt(
							segment.replaceFirst("(.+?)(\\d+)", "$2")) + 1;
				}
				boolean resolved = false;
				/*
				 * If resolving last segment, we're just descending the defaults
				 * at the end of the path (so the last segment is, yes, already
				 * resolved)
				 */
				while (!resolved) {
					resolved |= resolvingLastSegment;
					if (cursor.isCollection()) {
						Class elementClass = null;
						if (state.options.shortPaths) {
							Map<String, Class> typeMap = getAliasClassMap(
									cursor);
							elementClass = typeMap.get(segmentPath);
							if (elementClass != null) {
								resolved = true;
							} else {
								elementClass = typeMap.get("");
							}
							Object childValue = ensureNthCollectionElement(
									elementClass, index,
									(Collection) cursor.value);
							cursor = new Node(cursor, childValue, null);
							cursor.path.index = new CollectionIndex(
									elementClass, index, false);
						} else {
							throw new UnsupportedOperationException();
						}
					} else {
						Property property = null;
						if (state.options.shortPaths) {
							Map<String, Property> segmentMap = getAliasPropertyMap(
									cursor);
							property = segmentMap.get(segment);
							if (property != null) {
								resolved = true;
							} else {
								property = segmentMap.get("");
							}
						} else {
							property = getProperties(cursor.value).stream()
									.filter(p -> p.getName().equals(segment))
									.findFirst().get();
							resolved = true;
						}
						cursor = new Node(cursor,
								Reflections.propertyAccessor().getPropertyValue(
										cursor.value, property.getName()),
								null);
						cursor.path.property = property;
					}
					if (resolved) {
						/*
						 * check we don't need to descend further down the
						 * default paths
						 * 
						 */
						boolean lastSegment = idx == segments.length - 1;
						resolvingLastSegment |= lastSegment;
						if (lastSegment && !cursor.isLeaf()) {
							resolved = false;
						}
					}
				}
			}
			cursor.putToObject(value);
		});
	}

	private Object ensureNthCollectionElement(Class elementClass, int index,
			Collection collection) {
		for (Object object : collection) {
			if (object.getClass() == elementClass) {
				if (--index == 0) {
					return object;
				}
			}
		}
		/*
		 * will always be accessed in index-ascending order
		 */
		Object object = Reflections.newInstance(elementClass);
		collection.add(object);
		return object;
	}

	private Map<String, Class> getAliasClassMap(Node cursor) {
		Function<? super Class, ? extends String> keyMapper = clazz -> {
			TypeSerialization typeSerialization = Reflections.classLookup()
					.getAnnotationForClass(clazz, TypeSerialization.class);
			if (typeSerialization != null) {
				return typeSerialization.value();
			} else {
				return clazz.getName();
			}
		};
		return deSerializationPropertyAliasClass
				.computeIfAbsent(cursor.path.property, p -> {
					PropertySerialization propertySerialization = Reflections
							.propertyAccessor().getAnnotationForProperty(
									cursor.parent.value.getClass(),
									PropertySerialization.class, p.getName());
					Class[] availableTypes = propertySerialization.childTypes();
					if (availableTypes.length == 0) {
						propertySerialization = Reflections.propertyAccessor()
								.getAnnotationForProperty(
										cursor.parent.parent.parent.value
												.getClass(),
										PropertySerialization.class,
										cursor.parent.parent.path.property
												.getName());
						availableTypes = propertySerialization
								.grandchildTypes();
					}
					Map<String, Class> map = new LinkedHashMap<>();
					for (int idx = 0; idx < availableTypes.length; idx++) {
						Class clazz = availableTypes[idx];
						String name = idx == 0 ? "" : keyMapper.apply(clazz);
						map.put(name, clazz);
					}
					return map;
				});
	}

	private Map<String, Property> getAliasPropertyMap(Node cursor) {
		Function<? super Property, ? extends String> keyMapper = p -> {
			PropertySerialization propertySerialization = Reflections
					.propertyAccessor()
					.getAnnotationForProperty(cursor.value.getClass(),
							PropertySerialization.class, p.getName());
			if (propertySerialization != null) {
				return propertySerialization.defaultProperty() ? ""
						: propertySerialization.name();
			} else {
				return p.getName();
			}
		};
		return deSerializationClassAliasProperty
				.computeIfAbsent(cursor.value.getClass(), clazz -> {
					return getProperties(cursor.value).stream()
							.collect(AlcinaCollectors.toKeyMap(keyMapper));
				});
	}

	private List<Property> getProperties(Object value) {
		return serializationProperties.computeIfAbsent(value.getClass(), v -> {
			BeanDescriptor descriptor = Reflections.beanDescriptorProvider()
					.getDescriptor(value);
			Property[] propertyArray = descriptor.getProperties();
			return Arrays.stream(propertyArray)
					.sorted(Comparator.comparing(Property::getName))
					.filter(property -> {
						if (property.getMutatorMethod() == null) {
							return false;
						}
						if (property.getAccessorMethod() == null) {
							return false;
						}
						String name = property.getName();
						if (Reflections.propertyAccessor()
								.getAnnotationForProperty(v.getClass(),
										AlcinaTransient.class, name) != null) {
							return false;
						}
						return true;
					}).collect(Collectors.toList());
		});
	}

	private Object getValue(Property property, Object value) {
		try {
			return property.getAccessorMethod().invoke(value, new Object[0]);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void serialize() {
		state.maybeWriteTypeInfo();
		while (!state.pending.isEmpty()) {
			/*
			 * FIFO
			 */
			Node node = state.pending.remove(0);
			Object value = node.value;
			if (isLeafValue(value)) {
				if (!Objects.equals(value, node.defaultValue)) {
					node.putValue(state);
				}
				state.mergeableNode = node;
				continue;
			}
			state.mergeableNode = null;
			Path existingPath = state.visitedObjects.put(value, node.path);
			if (existingPath != null) {
				throw new IllegalStateException(Ax.format(
						"Object %s - multiple references: \n\t%s\n\t%s ", value,
						existingPath, node));
			}
			if (value instanceof Collection) {
				Counter counter = new Counter();
				Collection valueCollection = (Collection) value;
				Collection defaultCollection = (Collection) node.defaultValue;
				/*
				 * Preconditions:
				 * 
				 * - if valueCollection contains enums, it must be a superset of
				 * defaultCollection
				 * 
				 * - if valueCollection contains TreeSerializables, either
				 * defaultCollection is empty or there's an onto per-class
				 * mapping from value(i.e. elements are unique per-class)
				 * 
				 * - if valueCollection contains any other type,
				 * defaultCollection must be empty
				 */
				if (defaultCollection.size() > 0) {
					if (valueCollection.isEmpty()) {
						throw new IllegalArgumentException(Ax
								.format("Illegal collection at %s", node.path));
					}
					Object test = valueCollection.iterator().next();
					if (CommonUtils.isEnumish(test)) {
						if (!valueCollection.containsAll(defaultCollection)) {
							throw new IllegalArgumentException(Ax.format(
									"Illegal collection at %s", node.path));
						}
					} else if (test instanceof TreeSerializable) {
						if (valueCollection.size() < defaultCollection.size()) {
							throw new IllegalArgumentException(Ax.format(
									"Illegal collection - missing default - at %s",
									node.path));
						}
						Set<Class> seenClasses = new HashSet<>();
						Set<Class> defaultClasses = (Set) defaultCollection
								.stream().map(Object::getClass)
								.collect(Collectors.toSet());
						for (Object element : valueCollection) {
							if (!seenClasses.add(element.getClass())) {
								throw new IllegalArgumentException(Ax.format(
										"Illegal collection - multiple same-class elements - at %s",
										node.path));
							}
						}
						for (Object element : defaultCollection) {
							if (!seenClasses.contains(element.getClass())) {
								throw new IllegalArgumentException(Ax.format(
										"Illegal collection - missing default element %s - at %s",
										element, node.path));
							}
						}
					} else {
						throw new IllegalArgumentException(Ax
								.format("Illegal collection at %s", node.path));
					}
				}
				((Collection) value).forEach(childValue -> {
					Object defaultValue = null;
					if (CommonUtils.isEnumish(childValue)) {
						defaultValue = defaultCollection.contains(childValue)
								? childValue
								: null;
					} else if (childValue instanceof TreeSerializable) {
						for (Object element : defaultCollection) {
							if (element.getClass() == childValue.getClass()) {
								defaultValue = element;
								break;
							}
						}
						if (defaultValue == null) {
							defaultValue = Reflections.classLookup()
									.getTemplateInstance(childValue.getClass());
						}
					}
					Node childNode = new Node(node, childValue, defaultValue);
					childNode.path.index = counter.getAndIncrement(childValue);
					state.pending.add(childNode);
				});
			} else if (value instanceof TreeSerializable) {
				getProperties(value).forEach(property -> {
					Object childValue = getValue(property, value);
					Object defaultValue = getValue(property, node.defaultValue);
					Node childNode = new Node(node, childValue, defaultValue);
					childNode.path.property = property;
					childNode.path.propertySerialization = Reflections
							.propertyAccessor()
							.getAnnotationForProperty(node.value.getClass(),
									PropertySerialization.class,
									property.getName());
					state.pending.add(childNode);
				});
			} else {
				throw new UnsupportedOperationException(Ax.format(
						"Invalid value type: %s at %s", value, node.path));
			}
		}
	}

	public static class Options {
		boolean defaults;

		boolean shortPaths;

		boolean topLevelTypeInfo;

		public Options withDefaults(boolean defaults) {
			this.defaults = defaults;
			return this;
		}

		public Options withShortPaths(boolean shortPaths) {
			this.shortPaths = shortPaths;
			return this;
		}

		public Options withTopLevelTypeInfo(boolean topLevelTypeInfo) {
			this.topLevelTypeInfo = topLevelTypeInfo;
			return this;
		}
	}

	private static class CollectionIndex {
		Class clazz;

		int index;

		boolean leafValue = false;

		public CollectionIndex(Class clazz, int index, boolean leafValue) {
			this.clazz = clazz;
			this.index = index;
			this.leafValue = leafValue;
		}

		@Override
		public String toString() {
			return toStringFull();
		}

		public String toStringShort(Path path, boolean mergeLeafValuePaths) {
			if (leafValue) {
				return index == 1 || mergeLeafValuePaths ? ""
						: CommonUtils.padTwo(index - 1);
			}
			String strIndex = index == 1 ? "" : CommonUtils.padTwo(index - 1);
			String shortenedClassName = null;
			if (path != null) {
				if (path.parent.propertySerialization != null) {
					PropertySerialization propertySerialization = path.parent.propertySerialization;
					if (propertySerialization.childTypes().length > 0
							&& propertySerialization.childTypes()[0] == clazz) {
						shortenedClassName = "";
					}
				}
				if (path.parent.parent != null
						&& path.parent.parent.propertySerialization != null) {
					PropertySerialization propertySerialization = path.parent.parent.propertySerialization;
					if (propertySerialization.grandchildTypes().length > 0
							&& propertySerialization
									.grandchildTypes()[0] == clazz) {
						shortenedClassName = "";
					}
				}
			}
			if (shortenedClassName == null) {
				TypeSerialization typeSerialization = Reflections.classLookup()
						.getAnnotationForClass(clazz, TypeSerialization.class);
				if (typeSerialization != null) {
					shortenedClassName = typeSerialization.value();
				} else {
					shortenedClassName = clazz.getName().replace(".", "_");
				}
			}
			return shortenedClassName + strIndex;
		}

		String toStringFull() {
			if (leafValue) {
				return index == 1 ? "" : String.valueOf(index - 1);
			}
			String leafIndex = index == 1 ? "" : "" + (index - 1);
			return clazz.getName().replace(".", "_") + leafIndex;
		}
	}

	private static class Counter {
		Multimap<Class, List<Object>> indicies = new Multimap<Class, List<Object>>();

		CollectionIndex getAndIncrement(Object childValue) {
			Class clazz = childValue == null ? void.class
					: childValue.getClass();
			indicies.add(clazz, childValue);
			List<Object> list = indicies.get(clazz);
			return new CollectionIndex(clazz, list.size(),
					isLeafValue(childValue));
		}
	}

	static class Node {
		private static final String VALUE_SEPARATOR = ";";

		Path path;

		Class clazz;

		Object value;

		Object defaultValue;

		Node parent;

		public Node(Node parent, Object value, Object defaultValue) {
			this.parent = parent;
			this.path = new Path(parent == null ? null : parent.path);
			this.path.type = value == null ? null : value.getClass();
			this.value = value;
			this.defaultValue = defaultValue;
		}

		public String escapeValue(String str) {
			return TextUtils.Encoder.encodeURIComponentEsque(str);
		}

		public boolean isCollection() {
			return value instanceof Collection;
		}

		public boolean isLeaf() {
			if (value instanceof TreeSerializable) {
				return false;
			}
			if (value instanceof Collection) {
				return parent.value instanceof HasElementType;
			}
			return true;
		}

		public void putToObject(String stringValue) {
			Property property = path.property;
			// always leaf (primitiveish) values
			if (isCollection()) {
				Class elementClass = ((HasElementType) parent.value)
						.provideElementType();
				for (String leafStringValue : stringValue
						.split(VALUE_SEPARATOR)) {
					Object leafValue = parseStringValue(elementClass,
							leafStringValue);
					((Collection) value).add(leafValue);
				}
			} else {
				Class valueClass = property.getType();
				Object leafValue = parseStringValue(valueClass, stringValue);
				Reflections.propertyAccessor().setPropertyValue(parent.value,
						property.getName(), leafValue);
			}
		}

		public void putValue(State state) {
			String existingValue = null;
			String leafValue = toStringValue();
			if (state.mergeableNode != null && state.options.shortPaths
					&& path.canMergeTo(state.mergeableNode.path)) {
				existingValue = state.keyValues.get(path.toString(true, true));
			}
			FormatBuilder fb = new FormatBuilder();
			fb.separator(VALUE_SEPARATOR);
			fb.appendIfNotBlank(existingValue, leafValue);
			String value = fb.toString();
			if (Ax.notBlank(value)) {
				state.keyValues.put(path.toString(state.options.shortPaths,
						state.options.shortPaths), value);
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s=%s", path, value);
		}

		public String toStringValue() {
			if (value == null) {
				return NULL_MARKER;
			}
			if (value instanceof Date) {
				return String.valueOf(((Date) value).getTime());
			} else if (value instanceof String) {
				return escapeValue(value.toString());
			} else {
				return value.toString();
			}
		}

		Object parseStringValue(Class valueClass, String stringValue) {
			if (valueClass == String.class) {
				return TextUtils.Encoder.decodeURIComponentEsque(stringValue);
			}
			if (valueClass == Long.class || valueClass == long.class) {
				return Long.parseLong(stringValue);
			}
			if (valueClass == Double.class || valueClass == double.class) {
				return Double.valueOf(stringValue);
			}
			if (valueClass == Integer.class || valueClass == int.class) {
				return Integer.valueOf(stringValue);
			}
			if (valueClass == Boolean.class || valueClass == boolean.class) {
				return Boolean.valueOf(stringValue);
			}
			if (valueClass == Date.class) {
				value = new Date(Long.parseLong(stringValue));
			}
			if (valueClass.isEnum()) {
				return Enum.valueOf(valueClass, stringValue);
			}
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * Represents a path to a point in the object value tree
	 */
	static class Path {
		public Property property;

		public PropertySerialization propertySerialization;

		Path parent;

		CollectionIndex index = null;

		private transient String path;

		private transient String shortPath;

		public Class type;

		Path(Path parent) {
			this.parent = parent;
		}

		public boolean canMergeTo(Path other) {
			return toStringShort(true).equals(other.toStringShort(true));
		}

		@Override
		public String toString() {
			return toString(false, false);
		}

		String toString(boolean shortPaths, boolean mergeLeafValuePaths) {
			if (shortPaths) {
				return toStringShort(mergeLeafValuePaths);
			} else {
				return toStringFull();
			}
		}

		String toStringFull() {
			if (parent == null) {
				path = "";
			}
			if (path == null) {
				FormatBuilder fb = new FormatBuilder();
				fb.separator(".");
				fb.appendIfNotBlank(parent.toStringFull());
				if (property == null && index != null) {
					fb.appendIfNotBlank(index.toString());
				} else {
					fb.append(property.getName());
				}
				path = fb.toString();
			}
			return path;
		}

		String toStringShort(boolean mergeLeafValuePaths) {
			if (parent == null) {
				shortPath = "";
			}
			if (shortPath == null) {
				FormatBuilder fb = new FormatBuilder();
				fb.separator(".");
				fb.appendIfNotBlank(parent.toStringShort(mergeLeafValuePaths));
				if (property == null && index != null) {
					fb.appendIfNotBlank(
							index.toStringShort(this, mergeLeafValuePaths));
				} else {
					if (propertySerialization != null) {
						if (propertySerialization.defaultProperty()) {
						} else {
							fb.append(propertySerialization.name());
						}
					} else {
						fb.append(property.getName());
					}
				}
				shortPath = fb.toString();
			}
			return shortPath;
		}
	}

	static class State {
		public Node mergeableNode;

		StringMap keyValues = new StringMap();

		IdentityHashMap<Object, Path> visitedObjects = new IdentityHashMap();

		public Options options;

		List<Node> pending = new LinkedList<>();

		public void maybeWriteTypeInfo() {
			if (options.topLevelTypeInfo) {
				Node root = pending.get(0);
				keyValues.put(CLASS, root.value.getClass().getName());
				// FIXME - mvcc.flat - application.hash
				keyValues.put("#time",
						String.valueOf(System.currentTimeMillis()));
			}
		}
	}
}
