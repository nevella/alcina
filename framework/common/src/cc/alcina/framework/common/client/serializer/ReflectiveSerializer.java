package cc.alcina.framework.common.client.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.reflection.Annotations;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializers.PropertyIterator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.place.BasePlace;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * <p>
 * This class serializes possibly cyclic graphs to a javascript object. It
 * extensively from Jackson - including supporting async
 * serialization/deserialization, by using a stack-based serializtion structure
 * instead of recursion. Notional serialization algorithm is:
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
 * <ul>
 * </ul>
 * </ul>
 * 
 * WIP
 * 
 * @author nick@alcina.cc
 * 
 *         Note - will need to handle
 *         cc.alcina.framework.common.client.logic.domain.UserPropertyPersistable.
 *         Support specially
 * 
 *         check classloader usage in AlcinaBeanSerializer
 */
public class ReflectiveSerializer {
	private static Map<Class, TypeSerializer> typeSerializers = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	public static <T> T clone(T object) {
		return (T) deserialize(serialize(object));
	}

	public static <T> T deserialize(String value) {
		DeserializerOptions options = new DeserializerOptions();
		return deserialize(value, options);
	}

	public static <T> T deserialize(String value, DeserializerOptions options) {
		try {
			LooseContext.pushWithTrue(FlatTreeSerializer.CONTEXT_DESERIALIZING);
			if (value == null) {
				return null;
			}
			JsonSerialNode.ensureValueSerializers();
			State state = new State();
			state.deserializerOptions = options;
			// create json doc
			GraphNode node = new GraphNode(null, null, null);
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

	public static String serialize(Object object) {
		return serialize(object,
				new SerializerOptions().withElideDefaults(true));
	}

	public static String serialize(Object object, SerializerOptions options) {
		if (object == null) {
			return null;
		}
		JsonSerialNode.ensureValueSerializers();
		State state = new State();
		state.serializerOptions = options;
		GraphNode node = new GraphNode(null, null, null);
		node.state = state;
		node.setValue(object);
		SerialNode root = JsonSerialNode.empty();
		node.serialNode = root;
		node.serialNode.writeTypeName(object.getClass());
		node.writeValue();
		state.pending.add(node);
		ReflectiveSerializer serializer = new ReflectiveSerializer(state);
		serializer.serialize0();
		return root.toJson();
	}

	static TypeSerializer resolveSerializer(Class clazz) {
		if (typeSerializers.isEmpty()) {
			synchronized (typeSerializers) {
				if (typeSerializers.isEmpty()) {
					Registry.impls(TypeSerializer.class)
							.forEach(typeSerializer -> typeSerializer
									.handlesTypes()
									.forEach(handled -> typeSerializers
											.put(handled, typeSerializer)));
				}
			}
		}
		Class lookupClass = serializationClass(clazz);
		return typeSerializers.computeIfAbsent(lookupClass, k -> {
			List<Class> toResolve = Arrays.asList(lookupClass);
			while (toResolve.size() > 0) {
				Optional<Class> match = toResolve.stream()
						.filter(typeSerializers::containsKey).findFirst();
				if (match.isPresent()) {
					return typeSerializers.get(match.get());
				}
				List<Class> next = new ArrayList<>();
				toResolve.forEach(c2 -> {
					// implemented interfaces and superclass are at the same
					// level, so to speak
					if (c2.getSuperclass() != null) {
						next.add(c2.getSuperclass());
					}
					Reflections.classLookup().getInterfaces(c2)
							.forEach(next::add);
				});
				toResolve = next;
			}
			Bean bean = Annotations.resolve(lookupClass, Bean.class);
			if (bean != null) {
				return new ReflectiveTypeSerializer();
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

	State state;

	private ReflectiveSerializer(State state) {
		this.state = state;
	}

	private void deserialize(GraphNode root) {
		do {
			GraphNode node = state.pending.peek();
			node.readValue();
			Iterator<GraphNode> itr = node.iterator;
			if (Objects.equals(node.name, "memberUsers")) {
				int debug = 3;
			}
			if (itr != null && itr.hasNext()) {
				GraphNode next = itr.next();
				state.pending.push(next);
			} else {
				node.deserializationComplete();
				state.pending.pop();
			}
		} while (state.pending.size() > 0);
	}

	private void serialize0() {
		do {
			GraphNode node = state.pending.peek();
			node.ensureValue();
			if (node.depth() > 4) {
				int debug = 3;
			}
			Iterator<GraphNode> itr = node.iterator;
			if (itr != null && itr.hasNext()) {
				GraphNode next = itr.next();
				state.pending.push(next);
			} else {
				state.pending.pop();
			}
		} while (state.pending.size() > 0);
	}

	protected PropertySerialization getPropertySerialization(Class<?> clazz,
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

	public static class DeserializerOptions {
	}

	public static class ReflectiveTypeSerializer extends TypeSerializer {
		@Override
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
			child.propertyReflector.setPropertyValue(graphNode.value,
					child.value);
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
			return Reflections.newInstance(graphNode.type);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return incoming;
		}

		@Override
		public Iterator<GraphNode> writeIterator(GraphNode node) {
			Iterator<Property> iterator = SerializationSupport
					.getProperties(node.value).iterator();
			Object templateInstance = Reflections.classLookup()
					.getTemplateInstance(node.type);
			FilteringIterator<Property> filteringIterator = new FilteringIterator<>(
					iterator, p -> {
						if (!node.state.serializerOptions.elideDefaults) {
							return true;
						}
						Object childValue = Reflections.propertyAccessor()
								.getPropertyValue(node.value, p.getName());
						Object templateValue = Reflections.propertyAccessor()
								.getPropertyValue(templateInstance,
										p.getName());
						return !Objects.equals(childValue, templateValue);
					});
			return new MappingIterator<Property, GraphNode>(filteringIterator,
					new ReflectiveSerializer.GraphNodeMappingProperty(node));
		}

		@Override
		public void writeValueOrContainer(GraphNode node,
				SerialNode serialNode) {
			node.type = serializeAs(node.value.getClass());
			ReflectiveSerializer.SerialNode container = serialNode
					.createPropertyContainer();
			serialNode.write(node, container);
			node.serialNode = container;
		}
	}

	public static class SerializerOptions {
		boolean elideDefaults;

		public SerializerOptions withElideDefaults(boolean elideDefaults) {
			this.elideDefaults = elideDefaults;
			return this;
		}
	}

	@RegistryLocation(registryPoint = TypeSerializer.class)
	@Bean
	public static abstract class TypeSerializer {
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
		}

		public void deserializationComplete(GraphNode graphNode) {
			if (graphNode.parent != null) {
				graphNode.parent.serializer.childDeserializationComplete(
						graphNode.parent, graphNode);
			}
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

	@RegistryLocation(registryPoint = ValueSerializer.class)
	@Bean
	public static abstract class ValueSerializer<T> {
		public abstract List<Class> serializesTypes();

		protected T fromJson(Class<? extends T> clazz, JsonValue value) {
			switch (value.getType()) {
			case NULL:
				return null;
			case BOOLEAN:
				return fromJsonBoolean((JsonBoolean) value);
			case NUMBER:
				return fromJsonNumber((JsonNumber) value);
			case STRING:
				return fromJsonString(clazz, (JsonString) value);
			default:
				throw new UnsupportedOperationException();
			}
		}

		protected T fromJsonBoolean(JsonBoolean value) {
			throw new UnsupportedOperationException();
		}

		protected T fromJsonNumber(JsonNumber value) {
			throw new UnsupportedOperationException();
		}

		protected T fromJsonString(Class<? extends T> clazz, JsonString value) {
			throw new UnsupportedOperationException();
		}

		protected abstract JsonValue toJson(T object);
	}

	static class GraphNode {
		boolean consumedName;

		Object value;

		SerialNode serialNode;

		GraphNode parent;

		Iterator<GraphNode> iterator;

		TypeSerializer serializer;

		String name;

		PropertyReflector propertyReflector;

		Class type;

		State state;

		PropertySerialization propertySerialization;

		GraphNode(GraphNode parent, String name,
				PropertyReflector propertyReflector) {
			this.parent = parent;
			this.name = name;
			this.propertyReflector = propertyReflector;
			if (propertyReflector != null) {
				propertySerialization = Annotations.resolve(propertyReflector,
						PropertySerialization.class);
			}
			if (parent != null) {
				state = parent.state;
			}
		}

		public void deserializationComplete() {
			if (serializer != null) {
				serializer.deserializationComplete(this);
			} else {
				/*
				 * when deserializing an boject reference
				 */
				if (parent != null) {
					parent.serializer.childDeserializationComplete(parent,
							this);
				}
			}
		}

		public Class knownType() {
			if (propertyReflector == null) {
				return parentSerialization() != null
						&& parentSerialization().types().length == 1
								? parentSerialization().types()[0]
								: null;
			}
			Class type = propertyReflector.getPropertyType();
			if (Reflections.isEffectivelyFinal(type)) {
				return type;
			} else {
				return null;
			}
		}

		public void readValue() {
			if (value == null) {
				type = knownType();
				if (type == null) {
					int idx = serialNode.peekInt();
					if (idx != -1) {
						value = state.identityIdx.get(idx);
						return;
					}
				}
				if (type == null) {
					type = serialNode.readType(this);
				}
				serializer = resolveSerializer(type);
				value = serializer.readValue(this);
				if (serializer.isReferenceSerializer()) {
					state.identityIdx.put(state.identityIdx.size(), value);
				}
				iterator = serializer.readIterator(this);
			}
		}

		@Override
		public String toString() {
			String segment = Ax.format("[%s,%s]", name,
					value == null ? null : value.getClass().getSimpleName());
			return parent == null ? segment : parent.toString() + "." + segment;
		}

		private boolean hasFinalClass() {
			// TODO - 2022 - || Reflections.isFinal()
			if (value == null) {
				return true;
			}
			Class<? extends Object> clazz = value.getClass();
			if (Reflections.isEffectivelyFinal(clazz)) {
				/*
				 * still require explicit type info (since we may be in a
				 * collection - or array)
				 */
				if (propertyReflector != null) {
					return true;
				}
				return parentSerialization() != null
						&& parentSerialization().types().length == 1;
			}
			return false;
		}

		int depth() {
			return parent == null ? 0 : parent.depth() + 1;
		}

		void ensureValue() {
			if (serialNode == null) {
				if (serializer.isReferenceSerializer()) {
					Object idx = state.identityIdx.get(value);
					if (idx != null) {
						parent.serialNode.write(this, idx);
						return;
					}
				}
				if (!hasFinalClass()) {
					serialNode = parent.serialNode
							.writeClassValueContainer(name);
					consumedName = true;
					serialNode.writeTypeName(
							serializer.serializeAs(value.getClass()));
				}
				writeValue();
			}
		}

		PropertySerialization parentSerialization() {
			return parent == null ? null : parent.propertySerialization;
		}

		void setValue(Object value) {
			this.value = value;
			serializer = resolveSerializer(
					value == null ? void.class : value.getClass());
		}

		void writeValue() {
			serializer.writeValueOrContainer(this,
					serialNode != null ? serialNode : parent.serialNode);
			if (serializer.isReferenceSerializer()) {
				state.identityIdx.put(value, state.identityIdx.size());
			}
			iterator = serializer.writeIterator(this);
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
			GraphNode graphNode = new GraphNode(node, null, null);
			graphNode.setValue(t);
			return graphNode;
		}
	}

	static class GraphNodeMappingProperty
			implements Function<Property, GraphNode> {
		private GraphNode node;

		public GraphNodeMappingProperty(GraphNode node) {
			this.node = node;
		}

		@Override
		public GraphNode apply(Property t) {
			PropertyReflector propertyReflector = Reflections.propertyAccessor()
					.getPropertyReflector(node.value.getClass(), t.getName());
			GraphNode graphNode = new GraphNode(node, t.getName(),
					propertyReflector);
			graphNode.setValue(propertyReflector.getPropertyValue(node.value));
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
				// don't bother with synchronization
				Map<Class, ValueSerializer> valueSerializers = new LinkedHashMap<>();
				Registry.impls(ValueSerializer.class).stream()
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
			return (int) (jsonValue.getType() == JsonType.NUMBER
					? ((JsonNumber) jsonValue).asNumber()
					: -1);
		}

		@Override
		public Class readType(GraphNode node) {
			if (jsonValue.getType() == JsonType.NULL) {
				return void.class;
			}
			Preconditions.checkState(jsonValue.getType() == JsonType.ARRAY);
			JsonArray array = (JsonArray) jsonValue;
			String className = array.getString(0);
			JsonSerialNode valueChild = new JsonSerialNode(array.get(1));
			node.serialNode = valueChild;
			return Reflections.forName(className);
		}

		@Override
		public Object readValue(GraphNode node) {
			if (jsonValue.getType() == JsonType.NULL) {
				return null;
			}
			ValueSerializer valueSerializer = getValueSerializer(node.type);
			if (valueSerializer == null) {
				throw Ax.runtimeException("No value serializer for type %s",
						node.type);
			} else {
				return valueSerializer.fromJson(node.type, jsonValue);
			}
		}

		@Override
		public String toJson() {
			return jsonValue.toJson();
		}

		@Override
		public String toString() {
			return toJson();
		}

		@Override
		public void write(GraphNode node, Object value) {
			write(node.consumedName ? null : node.name, value);
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

		protected ValueSerializer
				getValueSerializer(Class<? extends Object> serializerType) {
			if (CommonUtils.isEnumOrEnumSubclass(serializerType)) {
				serializerType = Enum.class;
			}
			if (Reflections.isAssignableFrom(BasePlace.class, serializerType)) {
				serializerType = BasePlace.class;
			}
			ValueSerializer valueSerializer = valueSerializers
					.get(serializerType);
			return valueSerializer;
		}
	}

	interface SerialNode {
		boolean canWriteTypeName();

		SerialNode createArrayContainer();

		SerialNode createPropertyContainer();

		SerialNode getChild(int idx);

		SerialNode getChild(String key);

		String[] keys();

		int length();

		int peekInt();

		Class readType(GraphNode graphNode);

		Object readValue(GraphNode node);

		String toJson();

		void write(GraphNode node, Object value);

		SerialNode writeClassValueContainer(String name);

		void writeTypeName(Class type);
	}

	static class State {
		public Object value;

		// obj->id serializing, id->obj deserializing
		Map identityIdx = new IdentityHashMap<>();

		public SerializerOptions serializerOptions;

		IdentityHashMap<Object, Integer> visitedObjects = new IdentityHashMap();

		public DeserializerOptions deserializerOptions;

		Deque<GraphNode> pending = new LinkedList<>();
	}
}
