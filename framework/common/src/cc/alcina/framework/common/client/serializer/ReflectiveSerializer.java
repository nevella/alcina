package cc.alcina.framework.common.client.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Annotations;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.LooseContext;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
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
@SuppressWarnings("unused")
public class ReflectiveSerializer {
	private static Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	public static final Comparator<Property> PROPERTY_COMPARATOR = new Comparator<Property>() {
		@Override
		public int compare(Property f1, Property f2) {
			Class checkType = f1.getAccessorMethod().getDeclaringClass();
			boolean entityType = Reflections.isAssignableFrom(Entity.class,
					checkType);
			if (entityType) {
				/*
				 * serialize id, localid, other - to ensure population before
				 * hash
				 */
				int idx1 = f1.getName().equals("id") ? 0
						: f1.getName().equals("localId") ? 1 : 2;
				int idx2 = f2.getName().equals("id") ? 0
						: f2.getName().equals("localId") ? 1 : 2;
				if (idx1 != idx2) {
					return idx1 - idx2;
				}
			}
			return f1.getName().compareTo(f2.getName());
		}
	};

	private static Map<Class, TypeSerializer> typeSerializers = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	public static <T> T clone(T object) {
		return (T) deserialize(serialize(object));
	}

	public static Object deserialize(String value) {
		DeserializerOptions options = new DeserializerOptions();
		return deserialize(value, options);
	}

	public static Object deserialize(String value,
			DeserializerOptions options) {
		try {
			LooseContext.pushWithTrue(FlatTreeSerializer.CONTEXT_DESERIALIZING);
			if (value == null) {
				return null;
			}
			State state = new State();
			state.deserializerOptions = options;
			// create json doc
			GraphNode node = new GraphNode(null, null, null);
			new ReflectiveSerializer(state).deserialize(node);
			return node.value;
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
		GraphNode node = new GraphNode(null, null, object);
		node.serialNode = JsonSerialNode.empty();
		node.serialNode.writeTypeName(object);
		node.writeValue();
		state.pending.add(node);
		ReflectiveSerializer serializer = new ReflectiveSerializer(state);
		serializer.serialize0();
		return node.serialNode.toJson();
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
		return typeSerializers.computeIfAbsent(clazz, k -> {
			List<Class> toResolve = Arrays.asList(clazz);
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
			throw new IllegalArgumentException(
					Ax.format("No serializer for type %s", clazz));
		});
	}

	State state;

	private ReflectiveSerializer(State state) {
		this.state = state;
	}

	private void deserialize(GraphNode root) {
	}

	private List<Property> getProperties(Object value) {
		return serializationProperties.computeIfAbsent(value.getClass(),
				valueClass -> {
					BeanDescriptor descriptor = Reflections
							.beanDescriptorProvider().getDescriptor(value);
					Property[] propertyArray = descriptor.getProperties();
					return Arrays.stream(propertyArray).filter(property -> {
						if (property.getMutatorMethod() == null) {
							return false;
						}
						if (property.getAccessorMethod() == null) {
							return false;
						}
						String name = property.getName();
						if (Annotations.has(valueClass, name,
								AlcinaTransient.class)) {
							return false;
						}
						PropertySerialization propertySerialization = getPropertySerialization(
								valueClass, name);
						if (propertySerialization != null
								&& propertySerialization.ignore()) {
							return false;
						}
						return true;
					}).sorted(PROPERTY_COMPARATOR).collect(Collectors.toList());
				});
	}

	private void serialize0() {
		do {
			GraphNode node = state.pending.peek();
			node.ensureValue();
			Iterator<GraphNode> itr = node.ensureIterator();
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

	public static class SerializerOptions {
		boolean elideDefaults;

		public SerializerOptions withElideDefaults(boolean elideDefaults) {
			this.elideDefaults = elideDefaults;
			return this;
		}
	}

	@RegistryLocation(registryPoint = TypeSerializer.class)
	public static abstract class TypeSerializer {
		public abstract List<Class> handlesTypes();

		public abstract Class serializeAs(Class incoming);

		public abstract void writeValueOrContainer(GraphNode node,
				SerialNode serialNode);
	}

	public static class TypeSerializer_Collection extends TypeSerializer {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(Collection.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return ArrayList.class;
		}

		@Override
		public void writeValueOrContainer(GraphNode node,
				SerialNode serialNode) {
			serialNode.write(null, serialNode.createArrayContainer());
		}
	}

	public static class TypeSerializer_List extends TypeSerializer_Collection {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(List.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return ArrayList.class;
		}
	}

	static class GraphNode {
		public Object value;

		private PropertyReflector propertyReflector;

		SerialNode serialNode;

		private GraphNode parent;

		private Iterator<GraphNode> iterator;

		private TypeSerializer serializer;

		private String name;

		GraphNode(GraphNode parent, String name, Object value) {
			this.parent = parent;
			this.name = name;
			this.value = value;
			serializer = resolveSerializer(value.getClass());
		}

		private boolean hasFinalClass() {
			// TODO - 2022 - || Reflections.isFinal()
			return value == null
					|| Reflections.isEffectivelyFinal(value.getClass());
		}

		Iterator<GraphNode> ensureIterator() {
			return null;
		}

		void ensureValue() {
			if (serialNode == null) {
				if (!hasFinalClass()) {
					serialNode = parent.serialNode
							.writeClassValueContainer(name);
					serialNode.writeTypeName(value);
				}
				writeValue();
			}
		}

		void writeValue() {
			serializer.writeValueOrContainer(this,
					serialNode != null ? serialNode : parent.serialNode);
		}
	}

	static class JsonSerialNode implements SerialNode {
		private static Map<Class, ValueSerializer> valueSerializers;

		public static SerialNode empty() {
			JsonSerialNode serialNode = new JsonSerialNode();
			serialNode.jsonValue = Json.createArray();
			return serialNode;
		}

		public static void ensureValueSerializers() {
			if (valueSerializers == null) {
				// don't bother with synchronization
				Map<Class, ValueSerializer> valueSerializers = new LinkedHashMap<>();
				valueSerializers.put(String.class, new ValueSerializerString());
				valueSerializers.put(Boolean.class,
						new ValueSerializerBoolean());
				JsonSerialNode.valueSerializers = valueSerializers;
			}
		}

		public static SerialNode parse(String value) {
			throw new UnsupportedOperationException();
		}

		JsonValue jsonValue;

		@Override
		public boolean canWriteTypeName() {
			return false;
		}

		@Override
		public SerialNode createArrayContainer() {
			JsonSerialNode serialNode = new JsonSerialNode();
			serialNode.jsonValue = Json.createArray();
			return serialNode;
		}

		@Override
		public String toJson() {
			return jsonValue.toJson();
		}

		@Override
		public void write(String name, Object value) {
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
		public void writeTypeName(Object value) {
			Preconditions.checkState(jsonValue instanceof JsonArray);
			((JsonArray) jsonValue).set(0, value.getClass().getName());
		}

		private JsonValue toJsonValue(Object value) {
			if (value == null) {
				return Json.createNull();
			}
			if (value instanceof JsonSerialNode) {
				return ((JsonSerialNode) value).jsonValue;
			}
			Class<? extends Object> type = value.getClass();
			ValueSerializer valueSerializer = valueSerializers.get(type);
			if (valueSerializer == null) {
				throw Ax.runtimeException("No value serializer for type %s",
						type);
			} else {
				return valueSerializer.toJson(value);
			}
		}

		private static class ValueSerializerBoolean
				extends ValueSerializer<Boolean> {
			@Override
			public JsonValue toJson(Boolean object) {
				return Json.create(object);
			}

			@Override
			protected Boolean fromJsonBoolean(JsonBoolean value) {
				return value.asBoolean();
			}
		}

		private static class ValueSerializerString
				extends ValueSerializer<String> {
			@Override
			public JsonValue toJson(String object) {
				return Json.create(object);
			}

			@Override
			protected String fromJsonString(JsonString value) {
				return value.asString();
			}
		}

		static abstract class ValueSerializer<T> {
			protected T fromJson(JsonValue value) {
				if (value instanceof JsonNull) {
					return null;
				}
				if (value instanceof JsonBoolean) {
					return fromJsonBoolean((JsonBoolean) value);
				}
				if (value instanceof JsonNumber) {
					return fromJsonNumber((JsonNumber) value);
				}
				if (value instanceof JsonString) {
					return fromJsonString((JsonString) value);
				}
				throw new UnsupportedOperationException();
			}

			protected T fromJsonBoolean(JsonBoolean value) {
				throw new UnsupportedOperationException();
			}

			protected T fromJsonNumber(JsonNumber value) {
				throw new UnsupportedOperationException();
			}

			protected T fromJsonString(JsonString value) {
				throw new UnsupportedOperationException();
			}

			protected abstract JsonValue toJson(T object);
		}
	}

	interface SerialNode {
		boolean canWriteTypeName();

		SerialNode createArrayContainer();

		String toJson();

		void write(String name, Object value);

		SerialNode writeClassValueContainer(String name);

		void writeTypeName(Object value);
	}

	static class State {
		public Object value;

		public SerializerOptions serializerOptions;

		IdentityHashMap<Object, Integer> visitedObjects = new IdentityHashMap();

		public DeserializerOptions deserializerOptions;

		Deque<GraphNode> pending = new LinkedList<>();
	}
}
