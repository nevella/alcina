package cc.alcina.framework.common.client.serializer;

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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.Serializer;
import cc.alcina.framework.common.client.logic.reflection.Annotations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Base64;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

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
 * Polymporhpic collections must either define the complete list of allowable
 * members, to ensure alias uniqueness, or use any-type (unspecified - types()
 * annotation length==0) serialization - in the latter case the path compression
 * use the full classname as typeinfo.
 * 
 * </p>
 * <p>
 * Constraints: Collections cannot contain nulls. If a default property has no
 * type constraints, it must be populated by the constructor
 * </p>
 * 
 * <h2>Verification and safety:</h2>
 * <ul>
 * <li>Deserialization checks no property/name collisions.
 * <li>Deserialization checks for invalid types if types were specified for a
 * given property
 * <li>If with testing option, adds reachable TreeSerializable types to the
 * testing queue
 * </ul>
 *
 * <h2>TODO:</h2>
 * <ul>
 * <li>(later) GWT RPC (for serializing places, searchdefs etc without
 * serialization classes)
 * <li>Per-application implementations
 * </ul>
 * 
 *
 *
 * <h2>For: 2021.04.16</h2>
 * <ul>
 * <li>Check groupingparameters annotation
 * </ul>
 * 
 * 
 * @author nick@alcina.cc
 */
public class FlatTreeSerializer {
	private static final String CLASS = "class$";

	public static final String CONTEXT_DESERIALIZING = FlatTreeSerializer.class
			.getName() + ".CONTEXT_DESERIALIZING";

	public static final String CONTEXT_THROW_ON_SERIALIZATION_FAILURE = FlatTreeSerializer.class
			.getName() + ".CONTEXT_THROW_ON_SERIALIZATION_FAILURE";

	private static String NULL_MARKER = "__fts_NULL__";

	private static Map<Class, Map<String, Property>> deSerializationClassAliasProperty = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<Class, Boolean> assignableFromTreeSerializable = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<RootClassPropertyKey, Map<String, Class>> deSerializationPropertyAliasClass = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	public static Topic<StringPair> unequalSerialized = Topic.local();

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
		try {
			LooseContext.pushWithTrue(CONTEXT_DESERIALIZING);
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
			String mappedKeysValue = instance.treeSerializationCustomiser()
					.mapKeys(value, false);
			if (!Objects.equals(mappedKeysValue, value)) {
				state.keyValues = StringMap.fromPropertyString(mappedKeysValue);
				state.keyValues.remove(CLASS);
			}
			instance.treeSerializationCustomiser().onBeforeTreeDeserialize();
			Node node = new Node(null, instance, null);
			new FlatTreeSerializer(state).deserialize(node);
			instance.treeSerializationCustomiser().onAfterTreeDeserialize();
			return instance;
		} finally {
			LooseContext.pop();
		}
	}

	public static <T extends TreeSerializable> T deserialize(String value) {
		return deserialize(null, value);
	}

	public static boolean isDeserializing() {
		return LooseContext.is(CONTEXT_DESERIALIZING);
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
		State state;
		try {
			object.treeSerializationCustomiser().onBeforeTreeSerialize();
			state = new State();
			state.serializerOptions = options;
			Node node = new Node(null, object, Reflections.classLookup()
					.getTemplateInstance(object.getClass()));
			state.pending.add(node);
			FlatTreeSerializer serializer = new FlatTreeSerializer(state);
			serializer.serialize();
		} finally {
			object.treeSerializationCustomiser().onAfterTreeSerialize();
		}
		String serialized = state.keyValues.sorted(new KeyComparator())
				.toPropertyString();
		serialized = object.treeSerializationCustomiser().mapKeys(serialized,
				true);
		if (options.singleLine) {
			serialized = serialized.replace("\n", ":");
		}
		if (options.testSerialized) {
			DeserializerOptions deserializerOptions = new DeserializerOptions()
					.withShortPaths(options.shortPaths);
			TreeSerializable checkObject = deserialize(object.getClass(),
					serialized, deserializerOptions);
			SerializerOptions checkOptions = new SerializerOptions()
					.withElideDefaults(options.elideDefaults)
					.withShortPaths(options.shortPaths)
					.withSingleLine(options.singleLine)
					.withTopLevelTypeInfo(options.topLevelTypeInfo);
			String testSerialized = object.treeSerializationCustomiser()
					.filterTestSerialized(serialized);
			String checkSerialized = serialize(checkObject, checkOptions);
			String testCheckSerialized = object.treeSerializationCustomiser()
					.filterTestSerialized(checkSerialized);
			if (!Objects.equals(testSerialized, testCheckSerialized)) {
				unequalSerialized.publish(
						new StringPair(testSerialized, testCheckSerialized));
				Preconditions.checkState(
						Objects.equals(testSerialized, testCheckSerialized),
						"Unequal serialized:\n\n%s\n========\n%s", serialized,
						checkSerialized);
			}
			/*
			 * Would be nice - but then we'd need to do fancy things to
			 * replicate filterTestSerialized (handle fields we deliberately
			 * drop for client concision)
			 */
			// String reflectiveCheck0 = AlcinaBeanSerializer
			// .serializeHolder(object);
			// String reflectiveCheck1 = AlcinaBeanSerializer
			// .serializeHolder(checkObject);
			// if (!Objects.equals(reflectiveCheck0, reflectiveCheck1)) {
			// unequalSerialized.publish(
			// new StringPair(reflectiveCheck0, reflectiveCheck1));
			// Preconditions.checkState(
			// Objects.equals(reflectiveCheck0, reflectiveCheck1),
			// "Unequal serialized (bean):\n\n%s\n========\n%s",
			// reflectiveCheck0, reflectiveCheck1);
			// }
			// FIXME - 2021 - implement once reflectiveserializer up
		}
		return serialized;
	}

	public static String serializeElided(TreeSerializable object) {
		return serialize(object,
				new SerializerOptions().withTopLevelTypeInfo(true)
						.withShortPaths(true).withElideDefaults(true));
	}

	private static boolean isCollection(Class clazz) {
		return Reflections.isAssignableFrom(Collection.class, clazz);
	}

	private static boolean isIntermediateType(Class clazz) {
		return isCollection(clazz) || isTreeSerializable(clazz);
	}

	private static boolean isLeafValue(Object value) {
		if (value == null) {
			return true;
		}
		return isValueType(value.getClass());
	}

	// FIXME - support Class.isAssignableFrom in GWT
	private static boolean isTreeSerializable(Class clazz) {
		if (clazz == TreeSerializable.class) {
			return true;
		}
		return assignableFromTreeSerializable.computeIfAbsent(clazz, c -> {
			try {
				Object instance = Reflections.newInstance(clazz);
				return instance instanceof TreeSerializable;
			} catch (Exception e) {
				return false;
			}
		});
	}

	private static boolean
			isTreeSerializableWithInstantiationChecks(Class clazz) {
		if (clazz == TreeSerializable.class) {
			return true;
		}
		if (CommonUtils.stdAndPrimitives.contains(clazz)) {
			return false;
		}
		if (isCollection(clazz)) {
			return false;
		}
		return isTreeSerializable(clazz);
	}

	private static boolean isValueType(Class clazz) {
		if (clazz == Class.class) {
			return true;
		}
		if (CommonUtils.stdAndPrimitives.contains(clazz)) {
			return true;
		}
		if (Reflections.isAssignableFrom(Enum.class, clazz)) {
			return true;
		}
		if (clazz.isArray() && clazz.getComponentType() == byte.class) {
			return true;
		}
		if (isIntermediateType(clazz)) {
			return false;
		}
		if (Reflections.isAssignableFrom(Entity.class, clazz)) {
			return true;
		}
		if (Reflections.isAssignableFrom(BasePlace.class, clazz)) {
			return true;
		}
		return false;
	}

	State state;

	private MultikeyMap<Object> collectionElementClassIndexObject = new UnsortedMultikeyMap<>(
			3);

	private FlatTreeSerializer(State state) {
		this.state = state;
	}

	private <T> void addWithUniquenessCheck(Map<String, T> map, String key,
			T value, Node cursor) {
		T existingValue = map.put(key, value);
		if (existingValue != null) {
			throw new IllegalStateException(
					Ax.format("Key collision %s at path %s :: %s",
							state.rootClass.getSimpleName(), cursor.path, key));
		}
	}

	private void checkBranchUniqueness(List<String> resolvedSegments,
			Set<String> seenPossibleBranchesThisSegment, Set<String> newKeys) {
		for (String key : newKeys) {
			if (!seenPossibleBranchesThisSegment.add(key) && key.length() > 0) {
				throw new IllegalStateException(
						Ax.format("Key collision %s at path %s :: %s",
								state.rootClass.getSimpleName(),
								resolvedSegments, key));
			}
		}
	}

	private void checkPermittedType(Class elementClass,
			Map<String, Class> typeMap, Node cursor) {
		if (typeMap.size() > 0 && !typeMap.values().contains(elementClass)) {
			throw new IllegalStateException(
					Ax.format("Unexpected type %s at %s",
							elementClass.getName(), cursor.path));
		}
	}

	private void checkReachableTestingTypes(Node childNode) {
		if (state.serializerOptions.reachables != null
				&& childNode.path.propertySerialization != null) {
			for (Class clazz : childNode.path.propertySerialization.types()) {
				if (isTreeSerializableWithInstantiationChecks(clazz)) {
					state.serializerOptions.reachables.add(clazz);
				}
			}
		}
	}

	private void deserialize(Node root) {
		state.rootClass = root.value.getClass();
		/*
		 * (Document for 'short paths' case - non-short is simpler (does not use
		 * elision) For each path:
		 * 
		 * * Split to segments
		 * 
		 * * For each segment, either map to a property of the cursor object or
		 * descend the default property
		 * 
		 * * Once 'in' a property, resolve property type. Property type can be
		 * represented by:
		 * 
		 * * Empty string (one type only) * Type marker (multipe types)
		 * 
		 * * plus an index marker if property is a collection containing
		 * multiple elements of the same type
		 * 
		 * Once all segments are resolved, may still need to descend end-of-path
		 * defaults
		 * 
		 */
		for (Entry<String, String> entry : state.keyValues.entrySet()) {
			String path = entry.getKey();
			String value = entry.getValue();
			Node cursor = root;
			String[] segments = path.split("\\.");
			boolean resolvingLastSegment = false;
			String reachedPath = "";
			List<String> resolvedSegments = new ArrayList<>();
			int previousSegmentUnusedIndex = -1;
			boolean unknownProperty = false;
			for (int idx = 0; idx < segments.length; idx++) {
				String segment = segments[idx];
				boolean lastSegment = idx == segments.length - 1;
				SegmentIndex segmentIndex = new SegmentIndex(segment);
				int index = segmentIndex.index;
				String segmentPath = segmentIndex.segment;
				if (previousSegmentUnusedIndex != -1) {
					index = previousSegmentUnusedIndex;
					previousSegmentUnusedIndex = -1;
				}
				boolean resolved = false;
				Set<String> seenPossibleBranchesThisSegment = new LinkedHashSet<>();
				while (!resolved) {
					/*
					 * If resolving last segment, we're just descending the
					 * defaults at the end of the path (so the last segment is,
					 * yes, already resolved) - there may be several default
					 * steps so the 'descend until resolved' loop is correct,
					 * rather than attempting a short-circuit
					 */
					/*
					 * Note that we might be resolving >1 indexed collections
					 * (because of default paths), so set the index counter to 1
					 * after the firrst use - ditto previousSegmentUnusedIndex
					 */
					resolved |= resolvingLastSegment;
					if (cursor.isCollection() && !cursor.isLeaf()) {
						previousSegmentUnusedIndex = -1;
						Class elementClass = null;
						Map<String, Class> typeMap = getAliasClassMap(
								state.rootClass, cursor);
						if (state.deserializerOptions.shortPaths) {
							if (typeMap.isEmpty()) {
								elementClass = getClassFromSegment(segmentPath);
							} else {
								checkBranchUniqueness(resolvedSegments,
										seenPossibleBranchesThisSegment,
										typeMap.keySet());
								elementClass = typeMap.get(segmentPath);
								if (elementClass != null) {
									resolved = true;
								} else {
									elementClass = typeMap.get("");
								}
							}
							if (elementClass == null) {
								throw new IllegalStateException(
										Ax.format("Illegal type '%s' at %s",
												segmentPath, cursor.path));
							}
						} else {
							if (segmentPath.matches("\\d+")) {
								// leaf value index
							} else {
								elementClass = getClassFromSegment(segmentPath);
								checkPermittedType(elementClass, typeMap,
										cursor);
							}
							resolved = true;
						}
						try {
							Object childValue = ensureNthCollectionElement(
									elementClass, index,
									(Collection) cursor.value);
							cursor = new Node(cursor, childValue, null);
						} catch (RuntimeException e) {
							throw e;
						}
						cursor.path.index = new CollectionIndex(elementClass,
								null, index, false);
						index = 1;
					} else {
						if (index != 1) {
							previousSegmentUnusedIndex = index;
						}
						Property property = null;
						if (state.deserializerOptions.shortPaths) {
							Map<String, Property> segmentMap = getAliasPropertyMap(
									cursor);
							checkBranchUniqueness(resolvedSegments,
									seenPossibleBranchesThisSegment,
									segmentMap.keySet());
							property = segmentMap.get(segmentPath);
							if (property != null) {
								resolved = true;
							} else {
								property = segmentMap.get("");
							}
						} else {
							property = SerializationSupport
									.getProperties(cursor.value).stream()
									.filter(p -> p.getName()
											.equals(segmentPath))
									.findFirst().get();
							resolved = true;
						}
						if (property == null) {
							unknownProperty = true;
							LoggerFactory.getLogger(getClass()).info(
									"Unknown property: {} {}",
									state.rootClass.getSimpleName(), path);
							break;
						}
						PropertySerialization propertySerialization = SerializationSupport
								.getPropertySerialization(
										cursor.value.getClass(),
										property.getName());
						Object childValue = Reflections.propertyAccessor()
								.getPropertyValue(cursor.value,
										property.getName());
						Node lookahead = new Node(cursor, childValue, null);
						lookahead.path.property = property;
						lookahead.path.setPropertySerialization(
								propertySerialization);
						/*
						 * Always skip the lookahead type parameter segment in
						 * the segment loop.
						 */
						if (lookahead.isMultipleTypes()
								&& !lookahead.isCollection() && !lastSegment) {
							resolved = true;
							idx++;
							resolvedSegments.add(segments[idx]);
						}
						if (childValue == null) {
							if (lastSegment) {
								// nullable parameters cannot be intermediate
								// defaults
							} else {
								// ensure value (since not at leaf)
								Class type = null;
								Map<String, Class> typeMap = getAliasClassMap(
										state.rootClass, lookahead);
								if (lookahead.path.propertySerialization == null) {
									// no type info, only allow property type
									type = property.getType();
								} else {
									if (typeMap.size() != 1) {
										String typeSegment = segments[idx];
										if (state.deserializerOptions.shortPaths) {
											// no need to validate branch
											// uniqueness, no other path will
											// resolve to here
											if (typeMap.isEmpty()) {
												type = getClassFromSegment(
														typeSegment);
											} else {
												type = typeMap.get(typeSegment);
											}
										} else {
											type = getClassFromSegment(
													typeSegment);
											checkPermittedType(type, typeMap,
													cursor);
										}
									} else {
										type = lookahead.path.soleType();
									}
								}
								childValue = Reflections.newInstance(type);
								((TreeSerializable) childValue)
										.treeSerializationCustomiser()
										.onBeforeTreeDeserialize();
								Reflections.propertyAccessor().setPropertyValue(
										cursor.value, property.getName(),
										childValue);
							}
						}
						/*
						 * If null , definitely can't descend further for
						 * resolution (since null has no properties...). If at
						 * the end, though (lookahead.isLeaf()), change the
						 * cursor to the leaf setter
						 */
						if (childValue != null || lookahead.isLeaf()) {
							cursor = new Node(cursor, childValue, null);
							cursor.path.property = property;
							cursor.path.setPropertySerialization(
									propertySerialization);
						} else {
							resolved = true;
						}
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
						} else {
							resolvedSegments.add(segment);
						}
					}
				}
			}
			if (!unknownProperty) {
				cursor.putToObject(value);
			}
		}
	}

	private Object ensureNthCollectionElement(Class elementClass, int index,
			Collection collection) {
		IdentityWrapper identityWrapper = new IdentityWrapper(collection);
		if (!collectionElementClassIndexObject.containsKey(identityWrapper)) {
			// if the collection is non-empty, populate the lookup with its
			// initial elements
			collection.forEach(o -> {
				MultikeyMap<Object> existing = collectionElementClassIndexObject
						.asMapEnsure(true, identityWrapper, o.getClass());
				existing.put(existing.size() + 1, o);
			});
		}
		Object object = collectionElementClassIndexObject.get(identityWrapper,
				elementClass, index);
		if (object == null) {
			object = Reflections.newInstance(elementClass);
			collectionElementClassIndexObject.put(identityWrapper, elementClass,
					index, object);
			collection.add(object);
		}
		return object;
	}

	private Map<String, Class> getAliasClassMap(Class rootClass, Node cursor) {
		Function<? super Class, ? extends String> keyMapper = clazz -> {
			TypeSerialization typeSerialization = Annotations.resolve(clazz,
					TypeSerialization.class);
			if (typeSerialization != null
					&& !typeSerialization.value().isEmpty()) {
				return typeSerialization.value();
			} else {
				return clazz.getName();
			}
		};
		RootClassPropertyKey key = new RootClassPropertyKey(rootClass,
				cursor.path.property);
		return deSerializationPropertyAliasClass.computeIfAbsent(key, k -> {
			PropertySerialization propertySerialization = SerializationSupport
					.getPropertySerialization(cursor.parent.value.getClass(),
							k.property.getName());
			Class[] availableTypes = propertySerialization == null
					? new Class[0]
					: propertySerialization.types();
			boolean defaultType = availableTypes.length == 1;
			Map<String, Class> map = new LinkedHashMap<>();
			if (defaultType) {
				addWithUniquenessCheck(map, "", availableTypes[0], cursor);
			} else {
				for (int idx = 0; idx < availableTypes.length; idx++) {
					Class clazz = availableTypes[idx];
					String name = keyMapper.apply(clazz);
					addWithUniquenessCheck(map, name, clazz, cursor);
				}
			}
			return map;
		});
	}

	private Map<String, Property> getAliasPropertyMap(Node cursor) {
		Function<? super Property, ? extends String> keyMapper = property -> {
			PropertySerialization propertySerialization = SerializationSupport
					.getPropertySerialization(cursor.value.getClass(),
							property.getName());
			if (propertySerialization != null) {
				if (propertySerialization.defaultProperty()) {
					return "";
				}
				if (propertySerialization.path().length() > 0) {
					return propertySerialization.path();
				}
			}
			return property.getName();
		};
		Map<String, Property> map = new LinkedHashMap<>();
		return deSerializationClassAliasProperty
				.computeIfAbsent(cursor.value.getClass(), clazz -> {
					SerializationSupport.getProperties(cursor.value)
							.forEach(p -> addWithUniquenessCheck(map,
									keyMapper.apply(p), p, cursor));
					return map;
				});
	}

	private Class getClassFromSegment(String segmentPath) {
		return Reflections.classLookup()
				.getClassForName(segmentPath.replace("_", "."));
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
			Node cursor = state.pending.remove(0);
			Object value = cursor.value;
			if (isLeafValue(value)) {
				if (!Objects.equals(value, cursor.defaultValue)
						|| !state.serializerOptions.elideDefaults
						|| cursor.isPutDefaultValue()) {
					cursor.putValue(state);
				}
				state.mergeableNode = cursor;
				continue;
			}
			state.mergeableNode = null;
			Path existingPath = state.visitedObjects.put(value, cursor.path);
			if (existingPath != null) {
				throw new IllegalStateException(Ax.format(
						"Object %s - multiple references: \n\t%s\n\t%s ", value,
						existingPath, cursor));
			}
			Counter counter = new Counter();
			if (cursor.isCollection()) {
				Collection valueCollection = (Collection) value;
				cursor.path.verifyProvidesElementTypeInfo();
				Collection defaultCollection = cursor.defaultValue == null ?
				/* parent object is default null */
						CloneHelper.newCollectionInstance(valueCollection)
						: (Collection) cursor.defaultValue;
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
								childValue, existingPath, cursor));
					}
					Node childNode = new Node(cursor, childValue, defaultValue);
					childNode.path.index = counter.getAndIncrement(childValue);
					state.pending.add(childNode);
				});
			} else if (value instanceof TreeSerializable) {
				SerializationSupport.getProperties(value).forEach(property -> {
					Object childValue = getValue(cursor, property, value);
					Object defaultValue = cursor.defaultValue == null ? null
							: getValue(cursor, property, cursor.defaultValue);
					if (defaultValue == null && childValue != null
							&& childValue instanceof TreeSerializable) {
						defaultValue = Reflections
								.newInstance(childValue.getClass());
					}
					Node childNode = new Node(cursor, childValue, defaultValue);
					childNode.path.property = property;
					childNode.path.setPropertySerialization(SerializationSupport
							.getPropertySerialization(cursor.value.getClass(),
									property.getName()));
					if (childNode.path.ignoreForSerialization()) {
						return;
					}
					checkReachableTestingTypes(childNode);
					if (childNode.isMultipleTypes()
							&& !childNode.isCollection()) {
						/*
						 * So as to write (required) typeinfo to the path
						 */
						childNode.path.index = counter
								.getAndIncrement(childValue);
					}
					state.pending.add(childNode);
				});
			} else {
				// FIXME - remove check
				isLeafValue(value);
				throw new UnsupportedOperationException(Ax.format(
						"Invalid value type: %s at %s", value, cursor.path));
			}
		}
	}

	private Object synthesisePopulatedPropertyValue(Node node,
			Property property) {
		PropertySerialization propertySerialization = SerializationSupport
				.getPropertySerialization(node.value.getClass(),
						property.getName());
		if (propertySerialization != null
				&& propertySerialization.notTestable()) {
			try {
				return property.getAccessorMethod().invoke(node.value,
						new Object[0]);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		Class type = property.getType();
		if (propertySerialization == null || isValueType(type)) {
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
			if (propertySerialization.types().length > 0) {
				for (Class clazz : propertySerialization.types()) {
					if (isValueType(clazz)) {
						collection.add(synthesiseSimpleValue(clazz));
					} else {
						collection.add(Reflections.newInstance(clazz));
					}
				}
			} else {
				throw new UnsupportedOperationException();
			}
			return collection;
		}
		if (propertySerialization.types().length > 0) {
			type = propertySerialization.types()[0];
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
		if (valueClass == Class.class) {
			return String.class;
		}
		if (Reflections.isAssignableFrom(Enum.class, valueClass)) {
			Object object = valueClass.getEnumConstants()[0];
			if (object.toString().startsWith("__")
					&& valueClass.getEnumConstants().length > 1) {
				object = valueClass.getEnumConstants()[1];
			}
			return object;
		}
		if (Reflections.isAssignableFrom(VersionableEntity.class, valueClass)) {
			VersionableEntity entity = (VersionableEntity) Reflections
					.newInstance(valueClass);
			// deliberately non-persistent so that testing roundtrip is stable
			entity.setId(-1);
			return entity;
		}
		if (Reflections.isAssignableFrom(BasePlace.class, valueClass)) {
			BasePlace place = (BasePlace) Reflections.newInstance(valueClass);
			// deliberately non-persistent so that testing roundtrip is stable
			if (place instanceof EntityPlace) {
				((EntityPlace) place).id = -1;
			}
			return place;
		}
		if (valueClass.isArray()
				&& valueClass.getComponentType() == byte.class) {
			return new byte[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
		}
		if (Reflections.isAssignableFrom(TreeSerializable.class, valueClass)) {
			return Reflections.newInstance(valueClass);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public static class DeserializerOptions {
		boolean shortPaths;

		public DeserializerOptions withShortPaths(boolean shortPaths) {
			this.shortPaths = shortPaths;
			return this;
		}
	}

	public static class IdentityWrapper {
		private Object object;

		public IdentityWrapper(Object object) {
			this.object = object;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IdentityWrapper) {
				return object == ((IdentityWrapper) obj).object;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(object);
		}
	}

	public static class KeyComparator implements Comparator<String> {
		private Map<String, Key> map = new LinkedHashMap<>();

		@Override
		public int compare(String o1, String o2) {
			return get(o1).compareTo(get(o2));
		}

		private Key get(String s) {
			return map.computeIfAbsent(s, Key::new);
		}

		public static class Key implements Comparable<Key> {
			private List<SegmentIndex> segments;

			public Key(String path) {
				segments = Arrays.stream(path.split("\\."))
						.map(SegmentIndex::new).collect(Collectors.toList());
			}

			@Override
			public int compareTo(Key o) {
				for (int idx = 0; idx < segments.size(); idx++) {
					if (idx == o.segments.size()) {
						return 1;
					}
					SegmentIndex ours = segments.get(idx);
					SegmentIndex theirs = o.segments.get(idx);
					int segCmp = ours.compareTo(theirs);
					if (segCmp != 0) {
						return segCmp;
					}
				}
				return segments.size() == o.segments.size() ? 0 : -1;
			}

			@Override
			public String toString() {
				return segments.toString();
			}
		}
	}

	public static class MissingElementTypeException extends RuntimeException {
		public MissingElementTypeException(String message) {
			super(message);
		}
	}

	public static abstract class SerializerFlat extends Serializer {
		private transient static Logger logger = LoggerFactory
				.getLogger(SerializerFlat.class);

		public String beanSerialize(Object object,
				boolean hasClassNameProperty) {
			return super.serialize(object, hasClassNameProperty);
		}

		@Override
		public <V> V deserialize(String serialized, Class<V> clazz) {
			if (serialized == null) {
				return null;
			}
			if ((clazz != null && !serialized.startsWith("{"))
					|| serialized.contains("class$=")) {
				FlatTreeSerializer.DeserializerOptions options = new FlatTreeSerializer.DeserializerOptions()
						.withShortPaths(true);
				Class<? extends TreeSerializable> tsClazz = (Class<? extends TreeSerializable>) clazz;
				return (V) FlatTreeSerializer.deserialize(tsClazz, serialized,
						options);
			} else {
				return super.deserialize(serialized, clazz);
			}
		}

		@Override
		public String serialize(Object object, boolean hasClassNameProperty) {
			if (object == null) {
				return null;
			}
			TypeSerialization typeSerialization = Annotations
					.resolve(object.getClass(), TypeSerialization.class);
			boolean useFlat = object instanceof TreeSerializable
					&& (typeSerialization == null
							|| typeSerialization.flatSerializable());
			if (useFlat) {
				FlatTreeSerializer.SerializerOptions options = new FlatTreeSerializer.SerializerOptions()
						.withShortPaths(true)
						.withTopLevelTypeInfo(!hasClassNameProperty)
						.withTestSerialized(true).withElideDefaults(true);
				try {
					return FlatTreeSerializer
							.serialize((TreeSerializable) object, options);
				} catch (Exception e) {
					if (LooseContext
							.is(CONTEXT_THROW_ON_SERIALIZATION_FAILURE)) {
						throw new WrappedRuntimeException(e);
					}
					String jsonSerialized = super.serialize(object,
							hasClassNameProperty);
					logger.warn("SerializerFlat exception: {}", jsonSerialized);
					return jsonSerialized;
				}
			} else {
				return beanSerialize(object, hasClassNameProperty);
			}
		}
	}

	public static class SerializerOptions {
		boolean elideDefaults;

		boolean shortPaths;

		boolean topLevelTypeInfo;

		boolean singleLine;

		boolean testSerialized;

		boolean testSerializedPopulateAllPaths;

		Reachables reachables;

		public SerializerOptions withElideDefaults(boolean elideDefaults) {
			this.elideDefaults = elideDefaults;
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

		public SerializerOptions
				withTestSerializedReachables(Reachables reachables) {
			this.testSerializedPopulateAllPaths = true;
			this.reachables = reachables;
			return this;
		}

		public SerializerOptions
				withTopLevelTypeInfo(boolean topLevelTypeInfo) {
			this.topLevelTypeInfo = topLevelTypeInfo;
			return this;
		}

		public static class Reachables {
			public Set<Class<? extends TreeSerializable>> traversed = new LinkedHashSet<>();

			public Set<Class<? extends TreeSerializable>> pending = new LinkedHashSet<>();

			// Given the implicit requirement that TreeSerializables can't form
			// containment loops, no need to add root types
			public void add(Class<? extends TreeSerializable> clazz) {
				if (!traversed.contains(clazz)) {
					pending.add(clazz);
				}
			}
		}
	}

	private static class CollectionIndex {
		Class clazz;

		int index;

		boolean leafValue = false;

		private List<Object> list;

		public CollectionIndex(Class clazz, List<Object> list, int index,
				boolean leafValue) {
			this.clazz = clazz;
			this.list = list;
			this.index = index;
			this.leafValue = leafValue;
		}

		@Override
		public String toString() {
			return toStringFull(false);
		}

		public String toStringShort(Path path, boolean mergeLeafValuePaths) {
			if (leafValue) {
				return index == 1 || mergeLeafValuePaths ? "" : pad();
			}
			String strIndex = index == 1 ? "" : "-" + pad();
			String shortenedClassName = null;
			if (path != null) {
				if (path.parent.propertySerialization != null) {
					PropertySerialization propertySerialization = path.parent.propertySerialization;
					if (propertySerialization.types().length == 1
							&& propertySerialization.types()[0] == clazz) {
						shortenedClassName = "";
					}
				}
			}
			if (shortenedClassName == null) {
				TypeSerialization typeSerialization = Annotations.resolve(clazz,
						TypeSerialization.class);
				if (typeSerialization != null
						&& typeSerialization.value().length() > 0) {
					shortenedClassName = typeSerialization.value();
				} else {
					shortenedClassName = clazz.getName().replace(".", "_");
				}
			}
			return shortenedClassName + strIndex;
		}

		private String pad() {
			int value = index - 1;
			String valString = String.valueOf(value);
			if (list != null && list.size() > 9) {
				int maxValue = list.size() - 1;
				StringBuilder sb = new StringBuilder();
				int pad = String.valueOf(maxValue).length()
						- valString.length();
				for (int idx = 0; idx < pad; idx++) {
					sb.append('0');
				}
				sb.append(valString);
				return sb.toString();
			} else {
				return valString;
			}
		}

		String toStringFull(boolean mergeLeafValuePaths) {
			if (leafValue) {
				return index == 1 || mergeLeafValuePaths ? ""
						: String.valueOf(index - 1);
			}
			String leafIndex = index == 1 ? "" : "-" + (index - 1);
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
			return new CollectionIndex(clazz, list, list.size(),
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

		public boolean isMultipleTypes() {
			return path.isMultipleTypes();
		}

		public boolean isPutDefaultValue() {
			return path.serializer != null
					&& !path.serializer.elideDefaultValues(value);
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
						&& path.propertySerialization.types().length == 1
						&& isValueType(path.propertySerialization.types()[0]);
			}
			return true;
		}

		Object parseStringValue(Class valueClass, String stringValue) {
			if (NULL_MARKER.equals(stringValue)) {
				return null;
			}
			if (path.serializer != null) {
				return path.serializer.deserializeValue(stringValue);
			}
			if (valueClass == String.class) {
				return TextUtils.Encoder.decodeURIComponentEsque(stringValue);
			}
			if (valueClass == Class.class) {
				return Reflections.forName(stringValue);
			}
			if (valueClass == Long.class || valueClass == long.class) {
				long id = Long.parseLong(stringValue);
				return id;
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
			if (Reflections.isAssignableFrom(Enum.class, valueClass)) {
				return CommonUtils.getEnumValueOrNull(valueClass, stringValue,
						true, null);
			}
			if (Reflections.isAssignableFrom(BasePlace.class, valueClass)) {
				return RegistryHistoryMapper.get().getPlace(stringValue);
			}
			if (Reflections.isAssignableFrom(VersionableEntity.class,
					valueClass)) {
				long id = Long.parseLong(stringValue);
				if (id < 0) {
					// testing, synthesised entity
					VersionableEntity newInstance = (VersionableEntity) Reflections
							.newInstance(valueClass);
					newInstance.setId(id);
					return newInstance;
				} else {
					return Domain.find(valueClass, id);
				}
			}
			if (valueClass.isArray()
					&& valueClass.getComponentType() == byte.class) {
				return Base64.decode(stringValue);
			}
			throw new UnsupportedOperationException();
		}

		void putToObject(String stringValue) {
			Property property = path.property;
			Class leafType = NULL_MARKER.equals(stringValue) ? void.class
					: path.soleType();
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
				for (String leafStringValue : stringValue
						.split(VALUE_SEPARATOR)) {
					Object leafValue = parseStringValue(leafType,
							leafStringValue);
					collection.add(leafValue);
				}
			} else {
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
				String pathString = path.toString(shortPaths, shortPaths);
				state.keyValues.put(pathString, value);
			}
		}

		String toStringValue() {
			if (value == null) {
				return NULL_MARKER;
			}
			if (path.serializer != null) {
				return path.serializer.serializeValue(value);
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
			} else if (value instanceof Class) {
				return ((Class) value).getCanonicalName();
			} else if (value instanceof BasePlace) {
				return ((BasePlace) value).toTokenString();
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

		private PropertySerialization.Serializer serializer;

		Path(Path parent) {
			this.parent = parent;
		}

		public boolean canMergeTo(Path other) {
			return toStringShort(true).equals(other.toStringShort(true));
		}

		public void setPropertySerialization(
				PropertySerialization propertySerialization) {
			this.propertySerialization = propertySerialization;
			if (propertySerialization != null && propertySerialization
					.serializer() != PropertySerialization.Serializer.None.class) {
				serializer = Reflections
						.newInstance(propertySerialization.serializer());
			}
		}

		public Class soleType() {
			if (propertySerialization != null
					&& propertySerialization.types().length > 0) {
				if (propertySerialization.types().length > 1) {
					throw new UnsupportedOperationException();
				}
				return propertySerialization.types()[0];
			}
			return property.getType();
		}

		@Override
		public String toString() {
			return toString(false, false);
		}

		public void verifyProvidesElementTypeInfo() {
			boolean valid = propertySerialization != null
					&& propertySerialization.types().length > 0;
			if (!valid) {
				throw new MissingElementTypeException(Ax.format(
						"Unable to determine element type for collection property %s.%s",
						parent.type.getSimpleName(), property.getName()));
			}
		}

		private void addMaybeWithoutSeparator(FormatBuilder fb, String path) {
			if (path.startsWith("-")) {
				// tricky bit of defaulting here - for searchdefinitions.
				// FIXME - 2022 - think about doing this better (although it
				// works as-is)
				fb.separator("");
			}
			fb.appendIfNotBlank(path);
			fb.separator(".");
		}

		boolean ignoreForSerialization() {
			return propertySerialization != null
					&& !propertySerialization.fromClient() && GWT.isClient();
		}

		boolean isMultipleTypes() {
			return propertySerialization != null
					&& propertySerialization.types().length > 1;
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
				if (property != null) {
					fb.append(property.getName());
				}
				if (index != null) {
					addMaybeWithoutSeparator(fb,
							index.toStringFull(mergeLeafValuePaths));
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
				if (property != null) {
					if (propertySerialization != null) {
						if (propertySerialization.defaultProperty()) {
						} else {
							if (propertySerialization.path().length() > 0) {
								fb.append(propertySerialization.path());
							} else {
								fb.append(property.getName());
							}
						}
					} else {
						fb.append(property.getName());
					}
				}
				if (index != null) {
					addMaybeWithoutSeparator(fb,
							index.toStringShort(this, mergeLeafValuePaths));
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

	static class SegmentIndex implements Comparable<SegmentIndex> {
		String segment;

		int index;

		public SegmentIndex(String segment) {
			int lastDashIdx = -1;
			boolean digitsAfterLastDash = false;
			for (int idx = 0; idx < segment.length(); idx++) {
				char c = segment.charAt(idx);
				if (c == '-') {
					digitsAfterLastDash = false;
					lastDashIdx = idx;
				} else if (c >= '0' && c <= '9') {
					digitsAfterLastDash = lastDashIdx != -1;
				} else {
					lastDashIdx = -1;
				}
			}
			if (lastDashIdx != -1 && digitsAfterLastDash) {
				this.segment = segment.substring(0, lastDashIdx);
				this.index = Integer
						.parseInt(segment.substring(lastDashIdx + 1)) + 1;
			} else {
				this.segment = segment;
				this.index = 1;
			}
		}

		@Override
		public int compareTo(SegmentIndex o) {
			int segCmp = segment.compareTo(o.segment);
			if (segCmp != 0) {
				return segCmp;
			}
			return index - o.index;
		}

		@Override
		public String toString() {
			return Ax.format("%s:%s", segment, index);
		}
	}

	static class State {
		public Class<? extends Object> rootClass;

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
