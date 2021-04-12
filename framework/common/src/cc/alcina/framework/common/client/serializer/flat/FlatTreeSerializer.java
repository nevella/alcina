package cc.alcina.framework.common.client.serializer.flat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Base64;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TextUtils;

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
 * <li>(later) GWT RPC (for serializing places, searchdefs etc without
 * serialization classes)
 * <li>Apps
 * </ul>
 * 
 * <h2>FIXME:</h2>
 * <ul>
 * <li>PropertySerialization to ResolvedPropertySerialization (which respects
 * Type.PropertySerialization, path.parent.PropertySerialization.grandchildTypes
 * <li>Clarify anything complicated with psuedocode
 * <li>Check that collision detection works
 * </ul>
 *
 * @author nick@alcina.cc
 */
public class FlatTreeSerializer {
	private static final String CLASS = "class$";

	public static final String CONTEXT_SUPPRESS_EXCEPTIONS = FlatTreeSerializer.class
			.getName() + ".CONTEXT_SUPPRESS_EXCEPTIONS";

	private static String NULL_MARKER = "__fts_NULL__";

	private static Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<Class, Map<String, Property>> deSerializationClassAliasProperty = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<RootClassPropertyKey, Map<String, Class>> deSerializationPropertyAliasClass = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	public static <R extends TreeSerializable> R clone(R object) {
		return deserialize(serialize(object));
	}

	public static <T extends TreeSerializable> T deserialize(Class<T> clazz,
			String value) {
		DeserializerOptions options = new DeserializerOptions()
				.withShortPaths(true);
		return deserialize(clazz, value, options);
	}

	public static <T extends TreeSerializable> T deserialize(Class<T> clazz,
			String value, DeserializerOptions options) {
		if (value == null) {
			return null;
		}
		State state = new State();
		state.deserializerOptions = options;
		if (!value.contains("\n")) {
			value = value.replace(":", "\n");
		}
		state.keyValues = StringMap.fromPropertyString(value);
		if (clazz == null) {
			clazz = Reflections.classLookup()
					.getClassForName(state.keyValues.get(CLASS));
		}
		state.keyValues.remove(CLASS);
		T instance = Reflections.newInstance(clazz);
		Node node = new Node(null, instance, null);
		new FlatTreeSerializer(state).deserialize(node);
		return (T) node.value;
	}

	public static <T extends TreeSerializable> T deserialize(String value) {
		return deserialize(null, value);
	}

	public static String serialize(TreeSerializable object) {
		return serialize(object, new SerializerOptions()
				.withTopLevelTypeInfo(true).withShortPaths(true));
	}

	public static String serialize(TreeSerializable object,
			SerializerOptions options) {
		if (object == null) {
			return null;
		}
		State state = new State();
		state.serializerOptions = options;
		Node node = new Node(null, object, Reflections.classLookup()
				.getTemplateInstance(object.getClass()));
		state.pending.add(node);
		FlatTreeSerializer serializer = new FlatTreeSerializer(state);
		serializer.serialize();
		String serialized = state.keyValues.sorted().toPropertyString();
		if (options.singleLine) {
			serialized = serialized.replace("\n", ":");
		}
		if (options.testSerialized) {
			DeserializerOptions deserializerOptions = new DeserializerOptions()
					.withShortPaths(options.shortPaths);
			TreeSerializable checkObject = deserialize(object.getClass(),
					serialized, deserializerOptions);
			SerializerOptions checkOptions = new SerializerOptions()
					.withDefaults(options.defaults)
					.withShortPaths(options.shortPaths)
					.withSingleLine(options.singleLine)
					.withTopLevelTypeInfo(options.topLevelTypeInfo);
			String checkSerialized = serialize(checkObject, checkOptions);
			Preconditions.checkState(
					Objects.equals(serialized, checkSerialized),
					"Unequal serialized: \n%s\n%s", serialized,
					checkSerialized);
		}
		return serialized;
	}

	private static boolean isLeafValue(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof TreeSerializable) {
			return false;
		}
		Class<? extends Object> valueClass = value.getClass();
		if (CommonUtils.stdAndPrimitives.contains(valueClass)) {
			return true;
		}
		if (valueClass.isEnum() || CommonUtils.isEnumSubclass(valueClass)) {
			return true;
		}
		if (valueClass.isArray()
				&& valueClass.getComponentType() == byte.class) {
			return true;
		}
		if (value instanceof Entity) {
			return true;
		}
		return false;
	}

	private static boolean isValueClass(Class clazz) {
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
				boolean lastSegment = idx == segments.length - 1;
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
					if (cursor.toString().equals(
							"parameters.contactSearchDefinition.groupingParameters.columnOrders=[]")) {
						int debug = 3;
					}
					if (cursor.isCollection() && !cursor.isLeaf()) {
						Class elementClass = null;
						if (state.deserializerOptions.shortPaths) {
							Map<String, Class> typeMap = getAliasClassMap(
									root.value.getClass(), cursor);
							elementClass = typeMap.get(segmentPath);
							if (elementClass != null) {
								resolved = true;
							} else {
								elementClass = typeMap.get("");
							}
							try {
								Object childValue = ensureNthCollectionElement(
										elementClass, index,
										(Collection) cursor.value);
								cursor = new Node(cursor, childValue, null);
							} catch (RuntimeException e) {
								throw e;
							}
							cursor.path.index = new CollectionIndex(
									elementClass, index, false);
						} else {
							if (segmentPath.matches("\\d+")) {
								// leaf value index
							} else {
								elementClass = Reflections.classLookup()
										.getClassForName(
												segmentPath.replace("_", "."));
								try {
									Object childValue = ensureNthCollectionElement(
											elementClass, index,
											(Collection) cursor.value);
									cursor = new Node(cursor, childValue, null);
								} catch (RuntimeException e) {
									throw e;
								}
								cursor.path.index = new CollectionIndex(
										elementClass, index, false);
							}
							resolved = true;
						}
					} else {
						Property property = null;
						if (state.deserializerOptions.shortPaths) {
							Map<String, Property> segmentMap = getAliasPropertyMap(
									cursor);
							property = segmentMap.get(segment);
							if (property != null) {
								resolved = true;
							} else {
								property = segmentMap.get("");
							}
							if (property == null) {
								// REMOVE once ok
								deSerializationClassAliasProperty.clear();
								int debug = 3;
								getAliasPropertyMap(cursor);
							}
						} else {
							property = getProperties(cursor.value).stream()
									.filter(p -> p.getName().equals(segment))
									.findFirst().get();
							resolved = true;
						}
						Object childValue = Reflections.propertyAccessor()
								.getPropertyValue(cursor.value,
										property.getName());
						PropertySerialization propertySerialization = getPropertySerialization(
								cursor.value.getClass(), property.getName());
						if (!lastSegment && childValue == null) {
							// ensure value (since not at leaf)
							Class propertyType = property.getType();
							if (propertySerialization != null
									&& propertySerialization
											.leafType() != void.class) {
								propertyType = propertySerialization.leafType();
							}
							childValue = Reflections.newInstance(propertyType);
							Reflections.propertyAccessor().setPropertyValue(
									cursor.value, property.getName(),
									childValue);
						}
						cursor = new Node(cursor, childValue, null);
						cursor.path.property = property;
						cursor.path.propertySerialization = getPropertySerialization(
								cursor.parent.value.getClass(),
								property.getName());
					}
					if (resolved) {
						/*
						 * check we don't need to descend further down the
						 * default paths
						 * 
						 */
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

	private Map<String, Class> getAliasClassMap(Class rootClass, Node cursor) {
		Function<? super Class, ? extends String> keyMapper = clazz -> {
			TypeSerialization typeSerialization = Reflections.classLookup()
					.getAnnotationForClass(clazz, TypeSerialization.class);
			if (typeSerialization != null) {
				return typeSerialization.value();
			} else {
				return clazz.getName();
			}
		};
		RootClassPropertyKey key = new RootClassPropertyKey(rootClass,
				cursor.path.property);
		return deSerializationPropertyAliasClass.computeIfAbsent(key, k -> {
			PropertySerialization propertySerialization = getPropertySerialization(
					cursor.parent.value.getClass(), k.property.getName());
			Class[] availableTypes = propertySerialization.childTypes();
			boolean defaultType = availableTypes.length == 1;
			if (availableTypes.length == 0) {
				propertySerialization = getPropertySerialization(
						cursor.parent.parent.parent.value.getClass(),
						cursor.parent.parent.path.property.getName());
				availableTypes = propertySerialization.grandchildTypes();
				/*
				 * if both parent childTypes and grandchildTypes are length 1,
				 * force this (grandchildtype) to be non-default - otherwise we
				 * end up with blanks all the way down...
				 */
				defaultType = availableTypes.length == 1
						&& propertySerialization.childTypes().length != 1;
			}
			Map<String, Class> map = new LinkedHashMap<>();
			if (defaultType) {
				map.put("", availableTypes[0]);
			} else {
				for (int idx = 0; idx < availableTypes.length; idx++) {
					Class clazz = availableTypes[idx];
					String name = keyMapper.apply(clazz);
					map.put(name, clazz);
				}
			}
			return map;
		});
	}

	private Map<String, Property> getAliasPropertyMap(Node cursor) {
		Function<? super Property, ? extends String> keyMapper = p -> {
			PropertySerialization propertySerialization = getPropertySerialization(
					cursor.value.getClass(), p.getName());
			if (propertySerialization != null) {
				return propertySerialization.defaultProperty() ? ""
						: propertySerialization.path();
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
		return serializationProperties.computeIfAbsent(value.getClass(),
				valueClass -> {
					BeanDescriptor descriptor = Reflections
							.beanDescriptorProvider().getDescriptor(value);
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
										.getAnnotationForProperty(valueClass,
												AlcinaTransient.class,
												name) != null) {
									return false;
								}
								PropertySerialization propertySerialization = getPropertySerialization(
										valueClass, name);
								if (propertySerialization != null
										&& propertySerialization.ignore()) {
									return false;
								}
								return true;
							}).collect(Collectors.toList());
				});
	}

	private Object getValue(Node node, Property property, Object value) {
		if (state.serializerOptions.testSerializedPopulateAllPaths) {
			return synthesisePopulatedPropertyValue(node, property);
		}
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
				if (!Objects.equals(value, node.defaultValue)
						|| !state.serializerOptions.defaults) {
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
				node.path.verifyProvidesElementTypeInfo();
				Collection defaultCollection = node.defaultValue == null ?
				/* parent object is default null */
						CloneHelper.newCollectionInstance(valueCollection)
						: (Collection) node.defaultValue;
				((Collection) value).forEach(childValue -> {
					Object defaultValue = null;
					if (isLeafValue(childValue)) {
						//
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
					} else {
						throw new IllegalStateException(Ax.format(
								"Object %s - illegal in collection: \n\t%s\n\t%s ",
								childValue, existingPath, node));
					}
					Node childNode = new Node(node, childValue, defaultValue);
					childNode.path.index = counter.getAndIncrement(childValue);
					state.pending.add(childNode);
				});
			} else if (value instanceof TreeSerializable) {
				getProperties(value).forEach(property -> {
					Object childValue = getValue(node, property, value);
					Object defaultValue = node.defaultValue == null ? null
							: getValue(node, property, node.defaultValue);
					Node childNode = new Node(node, childValue, defaultValue);
					childNode.path.property = property;
					childNode.path.propertySerialization = getPropertySerialization(
							node.value.getClass(), property.getName());
					state.pending.add(childNode);
				});
			} else {
				throw new UnsupportedOperationException(Ax.format(
						"Invalid value type: %s at %s", value, node.path));
			}
		}
	}

	private Object synthesisePopulatedPropertyValue(Node node,
			Property property) {
		PropertySerialization propertySerialization = getPropertySerialization(
				node.value.getClass(), property.getName());
		Class type = property.getType();
		if (propertySerialization == null || isValueClass(type)) {
			/*
			 * get a value - any value
			 */
			return synthesiseSimpleValue(type);
		}
		Collection collection = null;
		if (type == Set.class) {
			collection = new LinkedHashSet<>();
		}
		if (type == List.class) {
			collection = new ArrayList<>();
		}
		if (collection != null) {
			if (propertySerialization.leafType() != void.class) {
				collection.add(synthesiseSimpleValue(
						propertySerialization.leafType()));
			} else if (propertySerialization.childTypes().length > 0) {
				for (Class clazz : propertySerialization.childTypes()) {
					collection.add(Reflections.newInstance(clazz));
				}
			} else if (node.parent != null
					&& node.parent.path.propertySerialization != null
					&& node.parent.path.propertySerialization
							.grandchildTypes().length > 0) {
				for (Class clazz : node.parent.path.propertySerialization
						.grandchildTypes()) {
					collection.add(Reflections.newInstance(clazz));
				}
			} else {
				throw new UnsupportedOperationException();
			}
			return collection;
		}
		if (propertySerialization.leafType() != void.class) {
			type = propertySerialization.leafType();
		} else {
			try {
				Object value = property.getAccessorMethod().invoke(node.value,
						new Object[0]);
				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		try {
			Object instance = Reflections.newInstance(type);
			if (instance instanceof TreeSerializable) {
				return instance;
			}
			if (instance instanceof Entity) {
				((Entity) instance).setId(1);
				return instance;
			}
		} catch (Exception e) {
			if (e instanceof InstantiationException) {
				throw new UnsupportedOperationException(Ax
						.format("Cannot determine serialization of %s", type));
			} else {
				throw WrappedRuntimeException.wrapIfNotRuntime(e);
			}
		}
		throw new UnsupportedOperationException();
	}

	private Object synthesiseSimpleValue(Class valueClass) {
		if (valueClass == String.class) {
			return "string-value";
		}
		if (valueClass == Long.class || valueClass == long.class) {
			return 1L;
		}
		if (valueClass == Double.class || valueClass == double.class) {
			return 1.0;
		}
		if (valueClass == Integer.class || valueClass == int.class) {
			return 1;
		}
		if (valueClass == Boolean.class || valueClass == boolean.class) {
			return true;
		}
		if (valueClass == Date.class) {
			return new Date();
		}
		if (valueClass.isEnum() || (valueClass.getSuperclass() != null
				&& valueClass.getSuperclass().isEnum())) {
			return valueClass.getEnumConstants()[0];
		}
		if (CommonUtils.hasSuperClass(valueClass, VersionableEntity.class)) {
			VersionableEntity entity = (VersionableEntity) Reflections
					.newInstance(valueClass);
			entity.setId(1);
			return entity;
		}
		if (valueClass.isArray()
				&& valueClass.getComponentType() == byte.class) {
			return new byte[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
		}
		try {
			Object instance = Reflections.newInstance(valueClass);
			if (instance instanceof TreeSerializable) {
				return instance;
			}
		} catch (Exception e) {
			if (e instanceof InstantiationException) {
				throw new UnsupportedOperationException(Ax.format(
						"Cannot determine serialization of %s", valueClass));
			} else {
				throw WrappedRuntimeException.wrapIfNotRuntime(e);
			}
		}
		throw new UnsupportedOperationException();
	}

	protected PropertySerialization getPropertySerialization(Class<?> clazz,
			String propertyName) {
		TypeSerialization typeSerialization = Reflections.classLookup()
				.getAnnotationForClass(clazz, TypeSerialization.class);
		if (typeSerialization != null) {
			for (PropertySerialization p : typeSerialization.properties()) {
				if (p.name().equals(propertyName)) {
					return p;
				}
			}
		}
		return Reflections.propertyAccessor().getAnnotationForProperty(clazz,
				PropertySerialization.class, propertyName);
	}

	public static class DeserializerOptions {
		boolean shortPaths;

		public DeserializerOptions withShortPaths(boolean shortPaths) {
			this.shortPaths = shortPaths;
			return this;
		}
	}

	public static class MissingElementTypeException extends RuntimeException {
		public MissingElementTypeException(String message) {
			super(message);
		}
	}

	public static class SerializerOptions {
		boolean defaults;

		boolean shortPaths;

		boolean topLevelTypeInfo;

		boolean singleLine;

		boolean testSerialized;

		private boolean testSerializedPopulateAllPaths;

		public SerializerOptions withDefaults(boolean defaults) {
			this.defaults = defaults;
			return this;
		}

		public SerializerOptions withShortPaths(boolean shortPaths) {
			this.shortPaths = shortPaths;
			return this;
		}

		public SerializerOptions withSingleLine(boolean singleLine) {
			this.singleLine = singleLine;
			return this;
		}

		public SerializerOptions withTestSerialized(boolean testSerialized) {
			this.testSerialized = testSerialized;
			return this;
		}

		public SerializerOptions withTestSerializedPopulateAllPaths(
				boolean testSerializedPopulateAllPaths) {
			this.testSerializedPopulateAllPaths = testSerializedPopulateAllPaths;
			return this;
		}

		public SerializerOptions
				withTopLevelTypeInfo(boolean topLevelTypeInfo) {
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
			return toStringFull(false);
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

		String toStringFull(boolean mergeLeafValuePaths) {
			if (leafValue) {
				return index == 1 || mergeLeafValuePaths ? ""
						: String.valueOf(index - 1);
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
		private static final String VALUE_SEPARATOR = ",";

		Path path;

		Object value;

		Object defaultValue;

		Node parent;

		boolean leafCollectionCleared;

		Node(Node parent, Object value, Object defaultValue) {
			this.parent = parent;
			this.path = new Path(parent == null ? null : parent.path);
			this.path.type = value == null ? null : value.getClass();
			this.value = value;
			this.defaultValue = defaultValue;
		}

		@Override
		public String toString() {
			return Ax.format("%s=%s", path, value);
		}

		String escapeValue(String str) {
			return TextUtils.Encoder.encodeURIComponentEsque(str);
		}

		boolean isCollection() {
			return value instanceof Collection;
		}

		boolean isLeaf() {
			if (value instanceof TreeSerializable) {
				return false;
			}
			if (value instanceof Collection) {
				return path.propertySerialization != null
						&& path.propertySerialization.leafType() != void.class;
			}
			return true;
		}

		Object parseStringValue(Class valueClass, String stringValue) {
			if (NULL_MARKER.equals(stringValue)) {
				return null;
			}
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
				return new Date(Long.parseLong(stringValue));
			}
			if (valueClass.isEnum() || (valueClass.getSuperclass() != null
					&& valueClass.getSuperclass().isEnum())) {
				return CommonUtils.getEnumValueOrNull(valueClass, stringValue,
						true, null);
			}
			if (CommonUtils.hasSuperClass(valueClass, Entity.class)) {
				return Domain.find(valueClass, Long.parseLong(stringValue));
			}
			if (value.getClass().isArray()
					&& value.getClass().getComponentType() == byte.class) {
				return Base64.decode(stringValue);
			}
			throw new UnsupportedOperationException();
		}

		void putToObject(String stringValue) {
			Property property = path.property;
			// always leaf (primitiveish) values
			if (isCollection()) {
				Collection collection = (Collection) value;
				/*
				 * Always clear any defaults for the leaf collection before
				 * first add
				 */
				if (!leafCollectionCleared) {
					collection.clear();
					leafCollectionCleared = true;
				}
				Class leafType = path.propertySerialization.leafType();
				for (String leafStringValue : stringValue
						.split(VALUE_SEPARATOR)) {
					Object leafValue = parseStringValue(leafType,
							leafStringValue);
					collection.add(leafValue);
				}
			} else {
				Class leafType = property.getType();
				if (path.propertySerialization != null
						&& path.propertySerialization
								.leafType() != void.class) {
					leafType = path.propertySerialization.leafType();
				}
				Object leafValue = parseStringValue(leafType, stringValue);
				Reflections.propertyAccessor().setPropertyValue(parent.value,
						property.getName(), leafValue);
			}
		}

		void putValue(State state) {
			String existingValue = null;
			String leafValue = toStringValue();
			boolean shortPaths = state.serializerOptions.shortPaths;
			if (state.mergeableNode != null
					&& path.canMergeTo(state.mergeableNode.path)) {
				existingValue = state.keyValues
						.get(path.toString(shortPaths, true));
			}
			FormatBuilder fb = new FormatBuilder();
			fb.separator(VALUE_SEPARATOR);
			fb.appendIfNotBlank(existingValue, leafValue);
			String value = fb.toString();
			if (Ax.notBlank(value)) {
				state.keyValues.put(path.toString(shortPaths, shortPaths),
						value);
			}
		}

		String toStringValue() {
			if (value == null) {
				return NULL_MARKER;
			}
			if (value instanceof Date) {
				return String.valueOf(((Date) value).getTime());
			} else if (value instanceof String) {
				return escapeValue(value.toString());
			} else if (value instanceof Entity) {
				Entity entity = (Entity) value;
				Preconditions.checkArgument(entity.domain().wasPersisted());
				return String.valueOf(entity.getId());
			} else if (CommonUtils.isEnumish(value)) {
				return value.toString().replace("_", "-").toLowerCase();
			} else if (value.getClass().isArray()
					&& value.getClass().getComponentType() == byte.class) {
				return Base64.encodeBytes((byte[]) value);
			} else {
				return value.toString();
			}
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

		public void verifyProvidesElementTypeInfo() {
			boolean valid = propertySerialization != null
					&& (propertySerialization.childTypes().length > 0
							|| propertySerialization.leafType() != void.class);
			if (!valid && parent != null && parent.parent != null) {
				valid = parent.parent.propertySerialization != null
						&& parent.parent.propertySerialization
								.grandchildTypes().length > 0;
			}
			if (!valid) {
				throw new MissingElementTypeException(Ax.format(
						"Unable to determine element type for collection property %s.%s",
						parent.type.getSimpleName(), property.getName()));
			}
		}

		String toString(boolean shortPaths, boolean mergeLeafValuePaths) {
			if (shortPaths) {
				return toStringShort(mergeLeafValuePaths);
			} else {
				return toStringFull(mergeLeafValuePaths);
			}
		}

		String toStringFull(boolean mergeLeafValuePaths) {
			if (parent == null) {
				path = "";
			}
			if (path == null) {
				FormatBuilder fb = new FormatBuilder();
				fb.separator(".");
				fb.appendIfNotBlank(parent.toStringFull(mergeLeafValuePaths));
				if (property == null && index != null) {
					fb.appendIfNotBlank(
							index.toStringFull(mergeLeafValuePaths));
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
							fb.append(propertySerialization.path());
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

	class RootClassPropertyKey {
		Class rootClass;

		Property property;

		public RootClassPropertyKey(Class rootClass, Property property) {
			super();
			this.rootClass = rootClass;
			this.property = property;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RootClassPropertyKey) {
				RootClassPropertyKey o = (RootClassPropertyKey) obj;
				return rootClass == o.rootClass && property == o.property;
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return Objects.hash(rootClass, property);
		}

		@Override
		public String toString() {
			return Ax.format("%s:%s", rootClass.getName(), property.getName());
		}
	}

	static class State {
		public SerializerOptions serializerOptions;

		public Node mergeableNode;

		StringMap keyValues = new StringMap();

		IdentityHashMap<Object, Path> visitedObjects = new IdentityHashMap();

		public DeserializerOptions deserializerOptions;

		List<Node> pending = new LinkedList<>();

		public void maybeWriteTypeInfo() {
			if (serializerOptions.topLevelTypeInfo) {
				Node root = pending.get(0);
				keyValues.put(CLASS, root.value.getClass().getName());
			}
		}
	}
}
