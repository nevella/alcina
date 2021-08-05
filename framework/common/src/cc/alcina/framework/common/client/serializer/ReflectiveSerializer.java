package cc.alcina.framework.common.client.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.LooseContext;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
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
			Node node = new Node(null, null, null);
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
		State state = new State();
		state.serializerOptions = options;
		Node node = new Node(null, null, object);
		node.documentPosition = JsonDocumentPosition.empty();
		node.documentPosition.writeTypeName(object.getClass());
		node.writeValue();
		state.pending.add(node);
		ReflectiveSerializer serializer = new ReflectiveSerializer(state);
		serializer.serialize0();
		return node.documentPosition.toJson();
	}

	static TypeSerializer resolve(Class clazz) {
		return null;
	}

	State state;

	private ReflectiveSerializer(State state) {
		this.state = state;
	}

	private void deserialize(Node root) {
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
			Node node = state.pending.peek();
			node.ensureValue();
			Iterator<Node> itr = node.ensureIterator();
			if (itr != null && itr.hasNext()) {
				Node next = itr.next();
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

		protected abstract void writeValueOrContainer(Node node, Object object);
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
		protected void writeValueOrContainer(Node node, Object object) {
			// TODO Auto-generated method stub
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

	interface DocumentPosition {
		boolean canWriteTypeName();

		String toJson();

		DocumentPosition writeClassValueContainer(String name);

		void writeTypeName(Object value);
	}

	static class JsonDocumentPosition implements DocumentPosition {
		public static DocumentPosition empty() {
			JsonDocumentPosition position = new JsonDocumentPosition();
			position.jsonValue = Json.createArray();
			return position;
		}

		public static DocumentPosition parse(String value) {
			throw new UnsupportedOperationException();
		}

		JsonValue jsonValue;

		@Override
		public boolean canWriteTypeName() {
			return false;
		}

		@Override
		public String toJson() {
			return jsonValue.toJson();
		}

		@Override
		public DocumentPosition writeClassValueContainer(String name) {
			JsonDocumentPosition position = new JsonDocumentPosition();
			position.jsonValue = Json.createArray();
			if (name != null) {
				JsonArray array = (JsonArray) jsonValue;
				array.set(array.length(), position.jsonValue);
			} else {
				JsonObject object = (JsonObject) jsonValue;
				object.put(name, position.jsonValue);
			}
			return position;
		}

		@Override
		public void writeTypeName(Object value) {
			Preconditions.checkState(jsonValue instanceof JsonArray);
			((JsonArray) jsonValue).set(0, value.getClass().getName());
		}
	}

	static class Node {
		public Object value;

		private PropertyReflector propertyReflector;

		DocumentPosition documentPosition;

		private Node parent;

		private Iterator<Node> iterator;

		private TypeSerializer serializer;

		private String name;

		Node(Node parent, String name, Object value) {
			this.parent = parent;
			this.name = name;
			this.value = value;
			serializer = resolve(value.getClass());
		}

		private boolean hasFinalClass() {
			// TODO - 2022 - Reflections.isFinal()
			return value == null
					|| Reflections.isEffectivelyFinal(value.getClass());
		}

		Iterator<Node> ensureIterator() {
			return null;
		}

		void ensureValue() {
			if (documentPosition == null) {
				if (!hasFinalClass()) {
					documentPosition = parent.documentPosition
							.writeClassValueContainer(name);
					documentPosition.writeTypeName(value);
				}
				writeValue();
			}
		}

		void writeValue() {
			serializer.writeValueOrContainer(this,
					documentPosition != null ? documentPosition
							: parent.documentPosition);
		}
	}

	static class State {
		public Object value;

		public SerializerOptions serializerOptions;

		IdentityHashMap<Object, Integer> visitedObjects = new IdentityHashMap();

		public DeserializerOptions deserializerOptions;

		Deque<Node> pending = new LinkedList<>();
	}
}
