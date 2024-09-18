package cc.alcina.framework.common.client.serializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypeBounds;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializers.PropertyIterator;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.place.BasePlace;
import elemental.js.json.JsJsonFactory;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * <p>
 * This class serializes possibly cyclic graphs to a javascript object. It
 * borrows extensively from Jackson, but also differs markedly: it supports
 * async (incremental) serialization/deserialization - by using a stack-based
 * serialization structure instead of recursion, it is gwt-compatible, and it
 * uses different controlling annotations. Notional serialization algorithm is:
 * </p>
 * <ul>
 * <li>For any unreached, non-data-like point x in the source graph:
 * <ul>
 * <li>Construct a state object, with a ref to the referent's state object
 * <li>An iterator (properties, key/value pairs, collection, array)
 * <li>Place on stack
 * </ul>
 * <li>Peek the topmost state object, get iterator value, begin serialization:
 * <ul>
 * <li>Write path info (property name or map key if all string keys)
 * <li>Write value info (array node and classinfo if ambiguous - i.e. non-final
 * and non-default)
 * <li>Write data-like value or back to 'for any unreached'
 * </ul>
 * <h4>TODO (optimisations)</h4>
 * <ul>
 * <li>Create a per-property serializer, to optimise bean
 * deserialization/serialization (done)
 * <li>Look at cost of primitive long serialization/deser (and box/unbox) in gwt
 * - possibly optimise
 * <li>Optionally (for rpc - where not concerned about in-process refactoring -
 * or for people who want clean json), add the following elision options:
 * <ul>
 * <li>Elide default implementation types (so type java.util.List,
 * implementation java.util.ArrayList not written) (done - serializeForRpc(),
 * deserializeRpc()
 * <li>Elide exact type (so if property type is nick.foo, implementation is of
 * type nick.foo, don't write, only write if implementation is
 * nick.foo_subclass) (harder - since requires lookahead. Probably don't do
 * this, to elide, make the type final.)
 *
 * </ul>
 * <li>Server-side - rather than using elemental.json, use streams (and avoid
 * stringbuilder by writing utf-8 directly)
 * </ul>
 * </ul>
 * 
 *
 * <h4>Notes - gotchas</h4>
 * <ul>
 * <li>If adding a value serializer, the value class should probably be in
 * {@link ClassReflector#stdAndPrimitivesMap}
 * <li>Don't serialize non-projected entities - use an {@link EntityLocator}
 * <li>Debug - if say the deserializer is complaining about x, it's possibly a
 * configuration mismatch between serializer and deserializer. So check those
 * first, then use [TODO: code sample] to track serialization at the actual
 * node, both sides
 * </ul>
 *
 * <p>
 * FIXME - speed - low - serialization to json should/could be custom (don't use
 * elemental)(write directly to outputstream)
 *
 * <p>
 * FIXME - speed - medium - serialization - get client/server metrics and a
 * baseline
 *
 * <p>
 * FIXME - dirndl 1x1f - use a ringbuffer (and fix ringbuffer rotation)
 *
 *
 */
@SuppressWarnings("deprecation")
public class ReflectiveSerializer {
	// FIXME - reflection - jsclassmap if client (and general switch to Maps)
	private static Map<Class, TypeSerializerForType> typeSerializers = Registry
			.impl(ConcurrentMapCreator.class).create();

	public static <T> T clone(T object) {
		return (T) deserialize(serialize(object));
	}

	public static <T> T deserialize(String value) {
		DeserializerOptions options = new DeserializerOptions();
		return deserialize(value, options);
	}

	public static <T> T deserializeRpc(String value) {
		DeserializerOptions options = new DeserializerOptions()
				.withDefaultCollectionTypes(true);
		return deserialize(value, options);
	}

	public static void registerClientDeserialization() {
		SerializationSupport.deserializationInstance.types = new TransienceContext[] {
				TransienceContext.CLIENT };
	}

	/**
	 * For use by clients which seriously prune the reachable reflective types
	 */
	public static Predicate<Property> applicationPropertyFilter;

	public static <T> T deserialize(String value, DeserializerOptions options) {
		try {
			LooseContext.pushWithTrue(Serializers.CONTEXT_DESERIALIZING);
			if (value == null) {
				return null;
			}
			JsonSerialNode.ensureValueSerializers();
			State state = new State(
					SerializationSupport.deserializationInstance,
					Resolver.get());
			state.deserializerOptions = options;
			// create json doc
			GraphNode node = new GraphNode(null, null);
			node.state = state;
			SerialNode root = JsonSerialNode.fromJson(value);
			node.serialNode = root;
			state.pending.add(node);
			new ReflectiveSerializer(state).deserialize(node);
			return (T) node.value;
		} finally {
			LooseContext.pop();
		}
	}

	// public for testing
	public static boolean hasSerializer(Class clazz) {
		try {
			resolveSerializer(clazz);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isSerializable(Class type) {
		return JsonSerialNode.getValueSerializer(type) != null;
	}

	static TypeSerializerForType resolveSerializer(Class clazz) {
		if (typeSerializers.isEmpty()) {
			synchronized (typeSerializers) {
				if (typeSerializers.isEmpty()) {
					Registry.query(TypeSerializer.class).implementations()
							.forEach(typeSerializer -> typeSerializer
									.handlesTypes().forEach(handled -> {
										TypeSerializerForType location = new TypeSerializerForType(
												handled, typeSerializer);
										typeSerializers.put(handled, location);
									}));
				}
			}
		}
		Class lookupClass = serializationClass(clazz);
		return typeSerializers.computeIfAbsent(lookupClass, k -> {
			List<Class<?>> toResolve = Arrays.asList(lookupClass);
			while (toResolve.size() > 0) {
				Optional<Class<?>> match = toResolve.stream()
						.filter(typeSerializers::containsKey).findFirst();
				if (match.isPresent()) {
					Class serializerType = match.get();
					return typeSerializers.get(serializerType);
				}
				List<Class<?>> next = new ArrayList<>();
				toResolve.forEach(c2 -> {
					// implemented interfaces and superclass are at the same
					// level, so to speak
					if (c2.getSuperclass() != null) {
						next.add(c2.getSuperclass());
					}
					Reflections.at(c2).getInterfaces().forEach(next::add);
				});
				toResolve = next;
			}
			boolean resolveWithReflectiveTypeSerializer = Reflections.at(clazz)
					.has(Bean.class);
			if (!GWT.isClient()) {
				resolveWithReflectiveTypeSerializer |= Reflections
						.isAssignableFrom(TreeSerializable.class, lookupClass)
						|| Reflections.isAssignableFrom(
								ReflectiveSerializable.class, lookupClass);
			}
			if (resolveWithReflectiveTypeSerializer) {
				return new TypeSerializerForType(null,
						new ReflectiveTypeSerializer());
			}
			throw new IllegalArgumentException(
					Ax.format("No serializer for type %s", lookupClass));
		});
	}

	static Class serializationClass(Class clazz) {
		Class lookupClass = clazz.getSuperclass() != null
				&& clazz.getSuperclass().isEnum()
						? clazz = clazz.getSuperclass()
						: clazz;
		return lookupClass;
	}

	public static String serialize(Object object) {
		return serialize(object,
				new SerializerOptions().withElideDefaults(true));
	}

	public static String serialize(Object object, SerializerOptions options) {
		try {
			LooseContext.pushWithTrue(Serializers.CONTEXT_SERIALIZING);
			return serialize0(object, options);
		} finally {
			LooseContext.pop();
		}
	}

	private static String serialize0(Object object, SerializerOptions options) {
		if (object == null) {
			return null;
		}
		JsonSerialNode.ensureValueSerializers();
		State state = new State(SerializationSupport.serializationInstance(),
				Resolver.get());
		state.serializerOptions = options;
		GraphNode node = new GraphNode(null, null);
		node.state = state;
		node.setValue(object);
		SerialNode root = JsonSerialNode.empty();
		node.serialNode = root;
		node.serialNode.writeTypeName(object.getClass());
		node.writeValue();
		state.pending.add(node);
		ReflectiveSerializer serializer = new ReflectiveSerializer(state);
		serializer.serialize0();
		SerialNode out = root;
		if (!options.topLevelTypeInfo) {
			out = root.getChild(1);
		}
		return out.toJson(options.pretty);
	}

	State state;

	private ReflectiveSerializer(State state) {
		this.state = state;
	}

	private void deserialize(GraphNode root) {
		do {
			GraphNode node = state.pending.peek();
			try {
				node.readValue();
				Iterator<GraphNode> itr = node.iterator;
				if (itr != null && itr.hasNext()) {
					GraphNode next = itr.next();
					state.pending.push(next);
				} else {
					node.deserializationComplete();
					state.pending.pop();
				}
			} catch (RuntimeException e) {
				if (node.state.deserializerOptions.continueOnException) {
					Throwable ex = WrappedRuntimeException.unwrap(e);
					if (node.parent != null
							&& node.parent.value instanceof HandlesDeserializationException) {
						String deserializationClassName = ex instanceof ClassNotFoundException
								? ex.getMessage()
								: null;
						String json = node.serialNode.toJson(true);
						DeserializationExceptionData deserializationExceptionData = new DeserializationExceptionData(
								ex, deserializationClassName, json);
						((HandlesDeserializationException) node.parent.value)
								.handleDeserializationException(
										deserializationExceptionData);
					}
					node.deserializationComplete();
					state.pending.pop();
				} else {
					throw new SerializationException(node, e);
				}
			}
		} while (state.pending.size() > 0);
	}

	/*
	 * Note that typeSerialization,properties _overrides_ on-property
	 * PropertySerialization (since the former is used as a subclass customiser)
	 */
	static PropertySerialization getPropertySerialization(Class<?> clazz,
			String propertyName) {
		TypeSerialization typeSerialization = Annotations.resolve(clazz,
				TypeSerialization.class);
		PropertySerialization annotation = null;
		if (typeSerialization != null) {
			for (PropertySerialization p : typeSerialization.properties()) {
				if (p.name().equals(propertyName)) {
					annotation = p;
					break;
				}
			}
		}
		if (annotation == null) {
			annotation = Annotations.resolve(clazz, propertyName,
					PropertySerialization.class);
		}
		return annotation;
	}

	private void serialize0() {
		do {
			GraphNode node = state.pending.peek();
			try {
				node.ensureValueWritten();
				Iterator<GraphNode> itr = node.iterator;
				if (itr != null && itr.hasNext()) {
					GraphNode next = itr.next();
					state.pending.push(next);
				} else {
					state.pending.pop();
				}
			} catch (RuntimeException e) {
				throw new SerializationException(node, e);
			}
		} while (state.pending.size() > 0);
	}

	/**
	 *
	 * Information for edge-case serialization checks
	 *
	 *
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Checks {
		boolean hasReflectedSubtypes() default false;

		boolean ignore() default false;
	}

	public static class DeserializerOptions {
		boolean continueOnException;

		boolean defaultCollectionTypes;

		public DeserializerOptions
				withDefaultCollectionTypes(boolean defaultCollectionTypes) {
			this.defaultCollectionTypes = defaultCollectionTypes;
			return this;
		}

		public DeserializerOptions
				withContinueOnException(boolean continueOnException) {
			this.continueOnException = continueOnException;
			return this;
		}
	}

	/*
	 * Models a java model node in the graph we're serializing + the
	 * corresponding json node
	 */
	static class GraphNode {
		boolean consumedName;

		Object value;

		SerialNode serialNode;

		GraphNode parent;

		Iterator<GraphNode> iterator;

		State state;

		PropertyNode propertyNode;

		TypeNode typeNode;

		GraphNode(GraphNode parent, PropertyNode propertyNode) {
			this.parent = parent;
			this.propertyNode = propertyNode;
			if (parent != null) {
				state = parent.state;
			}
		}

		int depth() {
			return parent == null ? 0 : parent.depth() + 1;
		}

		void deserializationComplete() {
			if (typeNode != null) {
				typeNode.serializer.deserializationComplete(this);
			} else {
				/*
				 * when deserializing an object reference
				 */
				if (parent != null) {
					parent.typeNode.serializer
							.childDeserializationComplete(parent, this);
				}
			}
		}

		void ensureValueWritten() {
			if (serialNode == null) {
				if (typeNode.serializer.isReferenceSerializer()) {
					Integer idx = state.identityIdx.get(value);
					if (idx != null) {
						parent.serialNode.write(this, idx);
						return;
					}
				}
				if (!hasFinalClass() && !state.serializerOptions
						.elideTypeInfo(propertyNode, value.getClass())) {
					serialNode = parent.serialNode
							.writeClassValueContainer(name());
					consumedName = true;
					serialNode.writeTypeName(
							typeNode.serializer.serializeAs(value.getClass()));
				}
				writeValue();
			}
		}

		TypeNode exactTypeNode() {
			if (propertyNode == null) {
				if (parent == null || parent.propertyNode == null) {
					return null;
				} else {
					return parent.propertyNode.exactChildTypeNode;
				}
			} else {
				return propertyNode.exactTypeNode;
			}
		}

		private boolean hasFinalClass() {
			if (value == null) {
				return true;
			}
			return exactTypeNode() != null;
		}

		String name() {
			return propertyNode == null ? null : propertyNode.name();
		}

		void readValue() {
			// Because the deserializer peeks, value may already have been
			// populated
			if (value == null) {
				typeNode = exactTypeNode();
				if (typeNode == null
						|| typeNode.serializer.isReferenceSerializer()) {
					int idx = serialNode.peekInt();
					if (idx != -1) {
						value = state.idxIdentity.get(idx);
						return;
					}
				}
				/*
				 * For legacy compatibility (where typeinfo was recorded but not
				 * expected), peek. Note that this does not handle serialNodes
				 * modelling Collections with unexpected classinfo, just those
				 * modelling Beans
				 */
				if (typeNode != null && typeNode.hasProperties()) {
					if (serialNode.isNotPropertyNode()) {
						// clear, and force a load of type info
						typeNode = null;
					}
				}
				if (typeNode == null) {
					Class type = serialNode.readType(this);
					typeNode = state.typeNode(type);
				}
				TypeSerializer serializer = typeNode.serializer;
				value = serializer.readValue(this);
				if (serializer.isReferenceSerializer()) {
					state.idxIdentity.put(state.idxIdentity.size(), value);
				}
				iterator = serializer.readIterator(this);
			}
		}

		void setValue(Object value) {
			this.value = value;
			if (value == null) {
				typeNode = state.voidTypeNode;
				return;
			}
			Class<? extends Object> type = value.getClass();
			if (propertyNode == null) {
				if (parent != null && parent.propertyNode != null) {
					typeNode = parent.propertyNode.childTypeNode(type);
				} else {
					typeNode = state.typeNode(type);
				}
			} else {
				typeNode = propertyNode.typeNode(type);
			}
		}

		@Override
		public String toString() {
			String segment = Ax.format("[%s,%s]", name(),
					value == null ? null : value.getClass().getSimpleName());
			return parent == null ? segment : parent.toString() + "." + segment;
		}

		public String toDebugString() {
			FormatBuilder format = new FormatBuilder();
			format.line("path: %s", toString());
			format.line("type node: %s", typeNode);
			format.line("property node:");
			format.indent(1);
			format.line(propertyNode);
			format.indent(0);
			format.line("parent json:\n%s",
					parent == null ? null : parent.serialNode.toJson(true));
			return format.toString();
		}

		void writeValue() {
			typeNode.serializer.writeValueOrContainer(this,
					serialNode != null ? serialNode : parent.serialNode);
			if (typeNode.serializer.isReferenceSerializer()) {
				state.identityIdx.put(value, state.identityIdx.size());
			}
			iterator = typeNode.serializer.writeIterator(this);
		}
	}

	static class GraphNodeMappingCollection
			implements Function<Object, GraphNode> {
		private GraphNode node;

		public GraphNodeMappingCollection(GraphNode node) {
			this.node = node;
		}

		@Override
		public GraphNode apply(Object t) {
			GraphNode graphNode = new GraphNode(node, null);
			graphNode.setValue(t);
			return graphNode;
		}
	}

	// refser - change to PropertyNode (the iteration should be across
	// propertyNodes)
	static class GraphNodeMappingProperty
			implements Function<PropertyNode, GraphNode> {
		private GraphNode node;

		private Map<PropertyNode, Object> propertyValues;

		public GraphNodeMappingProperty(GraphNode node,
				Map<PropertyNode, Object> propertyValues) {
			this.node = node;
			this.propertyValues = propertyValues;
		}

		@Override
		public GraphNode apply(PropertyNode propertyNode) {
			GraphNode graphNode = new GraphNode(node, propertyNode);
			graphNode.setValue(propertyValues.get(propertyNode));
			return graphNode;
		}
	}

	static class JsonSerialNode implements SerialNode {
		private static Map<Class, ValueSerializer> valueSerializers;

		public static SerialNode empty() {
			JsonSerialNode serialNode = new JsonSerialNode(Json.createArray());
			return serialNode;
		}

		public static void ensureValueSerializers() {
			if (valueSerializers == null) {
				// don't bother with synchronization - the map is immutable once
				// populated, so worst case is a few copies are made on init
				Map<Class, ValueSerializer> valueSerializers = AlcinaCollections
						.newLinkedHashMap();
				Registry.query(ValueSerializer.class).implementations()
						.forEach(vs -> vs.serializesTypes().forEach(
								t -> valueSerializers.put((Class) t, vs)));
				JsonSerialNode.valueSerializers = valueSerializers;
			}
		}

		public static SerialNode fromJson(String value) {
			JsonSerialNode serialNode = new JsonSerialNode(
					Json.instance().parse(value));
			return serialNode;
		}

		static ValueSerializer
				getValueSerializer(Class<? extends Object> valueType) {
			ValueSerializer valueSerializer = valueSerializers.get(valueType);
			if (valueSerializer == null) {
				if (ClassUtil.isEnumOrEnumSubclass(valueType)) {
					valueType = Enum.class;
				}
				if (Reflections.isAssignableFrom(BasePlace.class, valueType)) {
					valueType = BasePlace.class;
				}
				if (Reflections.isAssignableFrom(ExtensibleEnum.class,
						valueType)) {
					valueType = ExtensibleEnum.class;
				}
				valueSerializer = valueSerializers.get(valueType);
			}
			return valueSerializer;
		}

		public static SerialNode parse(String value) {
			throw new UnsupportedOperationException();
		}

		JsonValue jsonValue;

		public JsonSerialNode(JsonValue jsonValue) {
			this.jsonValue = jsonValue;
		}

		@Override
		public boolean canWriteTypeName() {
			return false;
		}

		@Override
		public SerialNode createArrayContainer() {
			JsonSerialNode serialNode = new JsonSerialNode(Json.createArray());
			return serialNode;
		}

		@Override
		public SerialNode createPropertyContainer() {
			JsonSerialNode serialNode = new JsonSerialNode(Json.createObject());
			return serialNode;
		}

		@Override
		public SerialNode getChild(int idx) {
			switch (jsonValue.getType()) {
			case ARRAY:
				JsonValue value = ((JsonArray) jsonValue).get(idx);
				return new JsonSerialNode(value);
			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public SerialNode getChild(String key) {
			switch (jsonValue.getType()) {
			case OBJECT:
				JsonValue value = ((JsonObject) jsonValue).get(key);
				return new JsonSerialNode(value);
			default:
				throw new UnsupportedOperationException();
			}
		}

		/*
		 * pure java 'jsonValue==null' was translated to .js '!jsonValue' --
		 * problematic when jsonValue === false
		 */
		private boolean isNullValue() {
			if (GWT.isScript()) {
				return JsJsonFactory.isNull(jsonValue);
			} else {
				return jsonValue.getType() == JsonType.NULL;
			}
		}

		@Override
		public String[] keys() {
			switch (jsonValue.getType()) {
			case OBJECT:
				return ((JsonObject) jsonValue).keys();
			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public int length() {
			switch (jsonValue.getType()) {
			case ARRAY:
				return ((JsonArray) jsonValue).length();
			case OBJECT:
				return ((JsonObject) jsonValue).keys().length;
			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public int peekInt() {
			return jsonValue == null || jsonValue.getType() != JsonType.NUMBER
					? -1
					: (int) jsonValue.asNumber();
		}

		@Override
		public Class readType(GraphNode node) {
			if (isNullValue()) {
				return void.class;
			}
			Preconditions.checkState(jsonValue.getType() == JsonType.ARRAY);
			JsonArray array = (JsonArray) jsonValue;
			String className = array.get(0).asString();
			JsonSerialNode valueChild = new JsonSerialNode(array.get(1));
			node.serialNode = valueChild;
			Class<?> forName = Reflections.forName(className);
			Preconditions.checkState(forName != null);
			return forName;
		}

		@Override
		public Object readValue(GraphNode node) {
			if (isNullValue()) {
				return null;
			}
			ValueSerializer valueSerializer = getValueSerializer(
					node.typeNode.type);
			if (valueSerializer == null) {
				throw Ax.runtimeException("No value serializer for type %s",
						node.typeNode.type);
			} else {
				return valueSerializer.fromJson(node.typeNode.type, jsonValue);
			}
		}

		@Override
		public String toJson(boolean pretty) {
			// FIXME - dirndl 1x2 - extend jsonValue.toJson, remove GWT.isScript
			// check
			return pretty && !GWT.isScript() ? JsonUtil.stringify(jsonValue, 2)
					: jsonValue.toJson();
		}

		private JsonValue toJsonValue(Object value) {
			if (value == null) {
				return Json.createNull();
			}
			if (value instanceof JsonSerialNode) {
				return ((JsonSerialNode) value).jsonValue;
			}
			Class<? extends Object> serializerType = value.getClass();
			ValueSerializer valueSerializer = getValueSerializer(
					serializerType);
			if (valueSerializer == null) {
				throw Ax.runtimeException("No value serializer for type %s",
						value.getClass());
			} else {
				return valueSerializer.toJson(value);
			}
		}

		@Override
		public String toString() {
			return toJson(true);
		}

		@Override
		public void write(GraphNode node, Object value) {
			write(node.consumedName ? null : node.name(), value);
		}

		private void write(String name, Object value) {
			JsonValue writeValue = toJsonValue(value);
			if (name == null) {
				JsonArray array = (JsonArray) jsonValue;
				array.set(array.length(), writeValue);
			} else {
				JsonObject object = (JsonObject) jsonValue;
				object.put(name, writeValue);
			}
		}

		@Override
		public SerialNode writeClassValueContainer(String name) {
			SerialNode serialNode = createArrayContainer();
			write(name, serialNode);
			return serialNode;
		}

		@Override
		public void writeTypeName(Class type) {
			Preconditions.checkState(jsonValue instanceof JsonArray);
			((JsonArray) jsonValue).set(0, serializationClass(type).getName());
		}

		@Override
		public boolean isNotPropertyNode() {
			switch (jsonValue.getType()) {
			case OBJECT:
				return false;
			default:
				return true;
			}
		}
	}

	/*
	 * Encapsulates serialization info for a property in the current
	 * serialization context.
	 */
	static class PropertyNode {
		TypeNode exactChildTypeNode;

		TypeNode exactTypeNode;

		/*
		 * These typenode fields are resolution optimisations
		 */
		TypeNode lastTypeNode;

		TypeNode lastChildTypeNode;

		Property property;

		PropertySerialization propertySerialization;

		private State state;

		public PropertyNode(State state, Property property) {
			this.state = state;
			this.property = property;
			Class type = property.getType();
			Class exactType = SerializationSupport
					.solePossibleImplementation(type);
			if (exactType != null) {
				exactTypeNode = state.typeNode(exactType);
			}
			if (state.deserializerOptions != null
					&& state.deserializerOptions.defaultCollectionTypes) {
				// must be synced with the the hardcoded handling in the
				// SerializerOptions.elideTypeInfo()
				// method
				if (type == List.class) {
					exactTypeNode = state.typeNode(ArrayList.class);
				} else if (type == Map.class) {
					exactTypeNode = state.typeNode(LinkedHashMap.class);
				} else if (type == Set.class) {
					exactTypeNode = state.typeNode(LinkedHashSet.class);
				}
			}
			propertySerialization = getPropertySerialization(
					property.getOwningType(), property.getName());
			if (propertySerialization != null
					&& propertySerialization.types().length == 1) {
				exactChildTypeNode = state
						.typeNode(propertySerialization.types()[0]);
			} else {
				if (Reflections.isAssignableFrom(Collection.class,
						property.getType())) {
					TypeBounds typeBounds = property.getTypeBounds();
					if (typeBounds.bounds.size() == 1) {
						Class<?> elementType = typeBounds.bounds.get(0);
						Class soleImplementationType = SerializationSupport
								.solePossibleImplementation(elementType);
						if (soleImplementationType != null) {
							exactChildTypeNode = state
									.typeNode(soleImplementationType);
						}
					}
				}
			}
		}

		public TypeNode childTypeNode(Class<? extends Object> type) {
			if (lastChildTypeNode == null || type != lastChildTypeNode.type) {
				lastChildTypeNode = state.typeNode(type);
			}
			return lastChildTypeNode;
		}

		String name() {
			return property.getName();
		}

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder();
			format.line(property);
			format.appendIfNotBlankKv("exactTypeNode", exactTypeNode);
			format.appendIfNotBlankKv("exactChildTypeNode", exactChildTypeNode);
			return format.toString();
		}

		public TypeNode typeNode(Class<? extends Object> type) {
			if (lastTypeNode == null || type != lastTypeNode.type) {
				lastTypeNode = state.typeNode(type);
				lastTypeNode.serializerLocation.verifyType(property.getType());
			}
			return lastTypeNode;
		}
	}

	public interface ReflectiveSerializable {
	}

	public static class ReflectiveTypeSerializer extends TypeSerializer {
		@Override
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
			child.propertyNode.property.set(graphNode.value, child.value);
		}

		@Override
		public boolean handlesDeclaredTypeSubclasses() {
			return true;
		}

		@Override
		public List<Class> handlesTypes() {
			return Collections.emptyList();
		}

		@Override
		public Iterator<GraphNode> readIterator(GraphNode node) {
			return new PropertyIterator(node);
		}

		@Override
		public Object readValue(GraphNode graphNode) {
			return graphNode.typeNode.newInstance();
		}

		@Override
		public Class serializeAs(Class incoming) {
			return incoming;
		}

		@Override
		public Iterator<GraphNode> writeIterator(GraphNode node) {
			// will become mostly TypeNode
			Iterator<PropertyNode> iterator = node.typeNode.properties
					.iterator();
			Object templateInstance = node.typeNode.templateInstance();
			// optimisation to avoid double-gets
			Map<PropertyNode, Object> propertyValues = AlcinaCollections
					.newHashMap();
			FilteringIterator<PropertyNode> filteringIterator = new FilteringIterator<>(
					iterator, propertyNode -> {
						Property property = propertyNode.property;
						Object childValue = property.get(node.value);
						propertyValues.put(propertyNode, childValue);
						// call later than needed (in this code block) to
						// guarantee propertyValues is
						// populated for all propertyNodes
						if (!node.state.serializerOptions.elideDefaults) {
							return true;
						}
						Object templateValue = property.get(templateInstance);
						return !Objects.equals(childValue, templateValue);
					});
			return new MappingIterator<PropertyNode, GraphNode>(
					filteringIterator,
					new ReflectiveSerializer.GraphNodeMappingProperty(node,
							propertyValues));
		}

		@Override
		public void writeValueOrContainer(GraphNode node,
				SerialNode serialNode) {
			node.typeNode = node.state
					.typeNode(serializeAs(node.value.getClass()));
			ReflectiveSerializer.SerialNode container = serialNode
					.createPropertyContainer();
			serialNode.write(node, container);
			node.serialNode = container;
		}
	}

	public static class SerializationException extends RuntimeException {
		public SerializationException(GraphNode node, RuntimeException e) {
			super(node.toDebugString(), e);
		}
	}

	public static class SerializerOptions {
		boolean elideDefaults;

		boolean topLevelTypeInfo = true;

		boolean typeInfo = true;

		boolean pretty;

		Set<Class> elideTypeInfo = Collections.emptySet();

		boolean defaultCollectionTypes;

		boolean elideTypeInfo(PropertyNode propertyNode,
				Class<? extends Object> valueType) {
			if (!typeInfo) {
				return true;
			}
			if (elideTypeInfo.contains(valueType)) {
				return true;
			}
			if (defaultCollectionTypes) {
				if (propertyNode != null) {
					// must be synced with the hardcoded handling in the
					// PropertyNode constructor. Note that the various immutable
					// collections/maps are allowed, and will be deserialized as
					// the default (mutable) types on the other end
					if (propertyNode.property.getType() == List.class) {
						Preconditions.checkState(valueType == ArrayList.class
								|| ClassUtil.isImmutableJdkCollectionType(
										valueType));
						return true;
					}
					if (propertyNode.property.getType() == Map.class) {
						Preconditions.checkState(
								valueType == LinkedHashMap.class || ClassUtil
										.isImmutableJdkCollectionType(
												valueType));
						return true;
					}
					if (propertyNode.property.getType() == Set.class) {
						Preconditions.checkState(
								valueType == LinkedHashSet.class || ClassUtil
										.isImmutableJdkCollectionType(
												valueType));
						return true;
					}
				}
			}
			return false;
		}

		public SerializerOptions withElideDefaults(boolean elideDefaults) {
			this.elideDefaults = elideDefaults;
			return this;
		}

		public SerializerOptions withElideTypeInfo(Set<Class> elideTypeInfo) {
			this.elideTypeInfo = elideTypeInfo;
			return this;
		}

		public SerializerOptions
				withDefaultCollectionTypes(boolean defaultCollectionTypes) {
			this.defaultCollectionTypes = defaultCollectionTypes;
			return this;
		}

		public SerializerOptions withPretty(boolean pretty) {
			this.pretty = pretty;
			return this;
		}

		public SerializerOptions
				withTopLevelTypeInfo(boolean topLevelTypeInfo) {
			this.topLevelTypeInfo = topLevelTypeInfo;
			return this;
		}

		public SerializerOptions withTypeInfo(boolean typeInfo) {
			this.typeInfo = typeInfo;
			return this;
		}
	}

	interface SerialNode {
		boolean canWriteTypeName();

		boolean isNotPropertyNode();

		SerialNode createArrayContainer();

		SerialNode createPropertyContainer();

		SerialNode getChild(int idx);

		SerialNode getChild(String key);

		String[] keys();

		int length();

		int peekInt();

		Class readType(GraphNode graphNode);

		Object readValue(GraphNode node);

		String toJson(boolean pretty);

		void write(GraphNode node, Object value);

		SerialNode writeClassValueContainer(String name);

		void writeTypeName(Class type);
	}

	static class State {
		TypeNode voidTypeNode;

		Object value;

		Map<Object, Integer> identityIdx = new IdentityHashMap<>();

		Map<Integer, Object> idxIdentity = new LinkedHashMap<>();

		SerializerOptions serializerOptions;

		IdentityHashMap<Object, Integer> visitedObjects = new IdentityHashMap();

		DeserializerOptions deserializerOptions;

		Deque<GraphNode> pending = new LinkedList<>();

		SerializationSupport serializationSupport;

		AnnotationLocation.Resolver resolver;

		Map<Class, TypeNode> typeNodes = AlcinaCollections.newHashMap();

		State(SerializationSupport serializationSupport, Resolver resolver) {
			this.serializationSupport = serializationSupport;
			this.resolver = resolver;
			voidTypeNode = typeNode(Void.class);
		}

		PropertyNode propertyNode(Property property) {
			return typeNode(property.getOwningType()).propertyNode(property);
		}

		TypeNode typeNode(Class type) {
			type = ClassUtil.isEnumSubclass(type) ? type.getSuperclass() : type;
			TypeNode typeNode = typeNodes.get(type);
			if (typeNode == null) {
				typeNode = new TypeNode(type);
				typeNodes.put(type, typeNode);
				typeNode.init(this);
			}
			return typeNode;
		}
	}

	/*
	 * Encapsulates serialization info for a type in the current serialization
	 * context. Every GraphNode has a type node - if the graphnode corresponds
	 * to Java null, the type node type will be Void
	 */
	static class TypeNode {
		Class<? extends Object> type;

		TypeSerializerForType serializerLocation;

		List<PropertyNode> properties = new ArrayList<>();

		Map<Property, PropertyNode> propertyMap = AlcinaCollections
				.newHashMap();

		Map<String, PropertyNode> propertyNameMap = AlcinaCollections
				.newHashMap();

		ClassReflector classReflector;

		TypeSerializer serializer;

		TypeNode(Class type) {
			this.type = type;
		}

		void init(State state) {
			classReflector = Reflections.at(type);
			serializerLocation = resolveSerializer(type);
			serializer = serializerLocation.typeSerializer;
			List<Property> list = state.serializationSupport
					.getProperties(type);
			for (Property property : list) {
				try {
					PropertyNode propertyNode = new PropertyNode(state,
							property);
					propertyMap.put(property, propertyNode);
					propertyNameMap.put(property.getName(), propertyNode);
					properties.add(propertyNode);
				} catch (Exception e) {
					Ax.out("Exclude: %s", property.toLocationString());
				}
			}
		}

		boolean hasProperties() {
			return properties.size() > 0;
		}

		Object newInstance() {
			return classReflector.newInstance();
		}

		PropertyNode propertyNode(Property property) {
			return propertyMap.get(property);
		}

		PropertyNode propertyNode(String name) {
			return propertyNameMap.get(name);
		}

		Object templateInstance() {
			return classReflector.templateInstance();
		}
	}

	@Reflected
	@Registration(TypeSerializer.class)
	public static abstract class TypeSerializer implements Registration.Ensure {
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
		}

		public void deserializationComplete(GraphNode graphNode) {
			if (graphNode.parent != null) {
				graphNode.parent.typeNode.serializer
						.childDeserializationComplete(graphNode.parent,
								graphNode);
			}
		}

		public boolean handlesDeclaredTypeSubclasses() {
			return false;
		}

		public abstract List<Class> handlesTypes();

		public boolean isReferenceSerializer() {
			return true;
		}

		public abstract Iterator<GraphNode> readIterator(GraphNode node);

		public abstract Object readValue(GraphNode graphNode);

		public abstract Class serializeAs(Class incoming);

		public abstract Iterator<GraphNode> writeIterator(GraphNode node);

		public abstract void writeValueOrContainer(GraphNode node,
				SerialNode serialNode);
	}

	static class TypeSerializerForType {
		Class actualType;

		TypeSerializer typeSerializer;

		public TypeSerializerForType(Class actualType,
				TypeSerializer typeSerializer) {
			this.actualType = actualType;
			this.typeSerializer = typeSerializer;
		}

		public void verifyType(Class declaredType) {
			if (declaredType != null
					&& !typeSerializer.handlesDeclaredTypeSubclasses()) {
				if (actualType != Enum.class && actualType != Entity.class
				// i.e. declaredtype is a supertype of serializertype
						&& !Reflections.isAssignableFrom(declaredType,
								actualType)) {
					throw new IllegalStateException(Ax.format(
							"Declared type %s cannot be serialized by resolved serializer for type %s",
							declaredType, actualType));
				}
			}
		}
	}

	@Reflected
	@Registration(ValueSerializer.class)
	public static abstract class ValueSerializer<T>
			implements Registration.Ensure {
		protected T fromJson(Class<? extends T> clazz, JsonValue value) {
			switch (value.getType()) {
			case NULL:
				return null;
			case BOOLEAN:
				return fromJsonBoolean(value);
			case NUMBER:
				return fromJsonNumber(value);
			case STRING:
				return fromJsonString(clazz, value);
			case ARRAY:
				// FIXME - this occurs when the type/value pair has not been
				// processed correctly (reading an interface property with an
				// enum value)
				JsonArray array = (JsonArray) value;
				return fromJsonString(clazz, array.get(1));
			default:
				throw new UnsupportedOperationException();
			}
		}

		protected T fromJsonBoolean(JsonValue value) {
			throw new UnsupportedOperationException();
		}

		protected T fromJsonNumber(JsonValue value) {
			throw new UnsupportedOperationException();
		}

		protected T fromJsonString(Class<? extends T> clazz, JsonValue value) {
			throw new UnsupportedOperationException();
		}

		public abstract List<Class> serializesTypes();

		protected abstract JsonValue toJson(T object);
	}

	public static String serializeForRpc(Object object) {
		return serialize(object, new SerializerOptions().withElideDefaults(true)
				.withDefaultCollectionTypes(true));
	}

	public interface HandlesDeserializationException {
		void handleDeserializationException(
				DeserializationExceptionData deserializationExceptionData);
	}

	public static class DeserializationExceptionData {
		public Throwable throwable;

		public String deserializationClassName;

		public String json;

		public DeserializationExceptionData(Throwable ex,
				String deserializationClassName, String json) {
			this.throwable = ex;
			this.deserializationClassName = deserializationClassName;
			this.json = json;
		}
	}
}
