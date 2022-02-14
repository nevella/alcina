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
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.logic.reflection.Annotations;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
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
 * <p>
 * TODO (optimisations)
 * </P>
 * <ul>
 * <li>Create a per-property serializer, to optimise bean
 * deserialization/serialization
 * <li>Look at cost of long serialization/deser (and box/unbox) in gwt -
 * possibly optimise
 * </ul>
 * </ul>
 *
 * @author nick@alcina.cc
 */
@SuppressWarnings("deprecation")
public class ReflectiveSerializer {
	private static Map<Class, TypeSerializer> typeSerializers = Registry
			.impl(ConcurrentMapCreator.class).create();

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
			state.serializationSupport = SerializationSupport.deserializationInstance;
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
		state.serializationSupport = SerializationSupport
				.serializationInstance();
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

	static TypeSerializer resolveSerializer(Class clazz, Class declaredType) {
		if (typeSerializers.isEmpty()) {
			synchronized (typeSerializers) {
				if (typeSerializers.isEmpty()) {
					Registry.query(TypeSerializer.class).implementations()
							.forEach(typeSerializer -> typeSerializer
									.handlesTypes()
									.forEach(handled -> typeSerializers
											.put(handled, typeSerializer)));
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
					TypeSerializer typeSerializer = typeSerializers
							.get(serializerType);
					if (declaredType != null && !typeSerializer
							.handlesDeclaredTypeSubclasses()) {
						if (serializerType != Enum.class
								&& serializerType != Entity.class
								&& !Reflections.isAssignableFrom(declaredType,
										serializerType)) {
							throw new IllegalStateException(Ax.format(
									"Declared type %s cannot be serialized by resolved serializer for type %s",
									declaredType, serializerType));
						}
					}
					return typeSerializer;
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
			Bean bean = Annotations.resolve(lookupClass, Bean.class);
			boolean resolveWithReflectiveTypeSerializer = bean != null;
			// FIXME - reflection - move isAssignable
			if (!GWT.isClient()) {
				resolveWithReflectiveTypeSerializer |= Reflections
						.isAssignableFrom(TreeSerializable.class, lookupClass)
						|| Reflections.isAssignableFrom(
								ReflectiveSerializable.class, lookupClass);
			}
			if (resolveWithReflectiveTypeSerializer) {
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
			node.ensureValueWritten();
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

	public interface ReflectiveSerializable {
	}

	public static class ReflectiveTypeSerializer extends TypeSerializer {
		@Override
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
			child.property.set(graphNode.value, child.value);
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
			return Reflections.newInstance(graphNode.type);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return incoming;
		}

		@Override
		public Iterator<GraphNode> writeIterator(GraphNode node) {
			Iterator<Property> iterator = node.state.serializationSupport
					.getProperties(node.value).iterator();
			Object templateInstance = Reflections.at(node.type)
					.templateInstance();
			FilteringIterator<Property> filteringIterator = new FilteringIterator<>(
					iterator, p -> {
						if (!node.state.serializerOptions.elideDefaults) {
							return true;
						}
						Object childValue = p.get(node.value);
						Object templateValue = p.get(templateInstance);
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

	
	@Bean
	@Registration(TypeSerializer.class)
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

	
	@Bean
	@Registration(ValueSerializer.class)
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

		Property property;

		Class type;

		State state;

		PropertySerialization propertySerialization;

		GraphNode(GraphNode parent, String name, Property property) {
			this.parent = parent;
			this.name = name;
			this.property = property;
			if (property != null) {
				propertySerialization = Annotations.resolve(property,
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
			Class type = null;
			if (property == null) {
				type = parentSerialization() != null
						&& parentSerialization().types().length == 1
								? parentSerialization().types()[0]
								: null;
			} else {
				// FIXME - dirndl 1.3 - use annotationlocation to resolve sole
				// type of property if possible
				type = property.getType();
			}
			if (type == null) {
				return null;
			}
			Class solePossibleImplementation = SerializationSupport
					.solePossibleImplementation(type);
			if (solePossibleImplementation != null) {
				return solePossibleImplementation;
			} else {
				return null;
			}
		}

		public void readValue() {
			if (value == null) {
				type = knownType();
				if (type == null || resolveSerializer(type, null)
						.isReferenceSerializer()) {
					int idx = serialNode.peekInt();
					if (idx != -1) {
						value = state.idxIdentity.get(idx);
						return;
					}
				}
				if (type == null) {
					type = serialNode.readType(this);
				}
				serializer = resolveSerializer(type, null);
				value = serializer.readValue(this);
				if (serializer.isReferenceSerializer()) {
					state.idxIdentity.put(state.idxIdentity.size(), value);
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
			if (value == null) {
				return true;
			}
			return knownType() != null;
		}

		int depth() {
			return parent == null ? 0 : parent.depth() + 1;
		}

		void ensureValueWritten() {
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
					value == null ? void.class : value.getClass(),
					property == null ? null : property.getType());
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
			Property property = Reflections.at(node.value.getClass())
					.property(t.getName());
			GraphNode graphNode = new GraphNode(node, t.getName(), property);
			graphNode.setValue(property.get(node.value));
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

		Map<Object, Integer> identityIdx = new IdentityHashMap<>();

		Map<Integer, Object> idxIdentity = new LinkedHashMap<>();

		public SerializerOptions serializerOptions;

		IdentityHashMap<Object, Integer> visitedObjects = new IdentityHashMap();

		public DeserializerOptions deserializerOptions;

		Deque<GraphNode> pending = new LinkedList<>();

		SerializationSupport serializationSupport;
	}
}
