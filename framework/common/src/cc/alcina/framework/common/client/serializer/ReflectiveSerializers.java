package cc.alcina.framework.common.client.serializer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MappingIterator;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.GraphNode;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.PropertyNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Base64;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import elemental.json.Json;
import elemental.json.JsonBoolean;
import elemental.json.JsonNumber;
import elemental.json.JsonString;
import elemental.json.JsonValue;

@SuppressWarnings("deprecation")
public class ReflectiveSerializers {
	public static class ReflectiveTypeSerializer_Entity
			extends ReflectiveSerializer.ReflectiveTypeSerializer {
		@Override
		public List<Class> handlesTypes() {
			return Collections.singletonList(Entity.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return Domain.resolveEntityClass(incoming);
		}
	}

	public static class TypeSerializer_Collection
			extends ReflectiveSerializer.TypeSerializer {
		@Override
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
			((Collection) graphNode.value).add(child.value);
		}

		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(Collection.class);
		}

		@Override
		public Iterator<GraphNode> readIterator(GraphNode node) {
			return new ArrayIterator(node);
		}

		@Override
		public Object readValue(GraphNode graphNode) {
			return graphNode.typeNode.newInstance();
		}

		@Override
		public Class serializeAs(Class incoming) {
			return ArrayList.class;
		}

		@Override
		public Iterator<ReflectiveSerializer.GraphNode>
				writeIterator(ReflectiveSerializer.GraphNode node) {
			Iterator iterator = ((Collection) node.value).iterator();
			return new MappingIterator<>(iterator,
					new ReflectiveSerializer.GraphNodeMappingCollection(node));
		}

		@Override
		public void writeValueOrContainer(ReflectiveSerializer.GraphNode node,
				ReflectiveSerializer.SerialNode serialNode) {
			ReflectiveSerializer.SerialNode container = serialNode
					.createArrayContainer();
			serialNode.write(node, container);
			node.serialNode = container;
		}
	}

	public static class TypeSerializer_CountingMap extends TypeSerializer_Map {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(CountingMap.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return CountingMap.class;
		}
	}

	public static class TypeSerializer_LightMap extends TypeSerializer_Map {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(LightMap.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return LightMap.class;
		}
	}

	public static class TypeSerializer_LightSet
			extends TypeSerializer_Collection {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(LightSet.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return LightSet.class;
		}
	}

	public static class TypeSerializer_LinkedList
			extends TypeSerializer_Collection {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(LinkedList.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return LinkedList.class;
		}
	}

	public static class TypeSerializer_LiSet extends TypeSerializer_Collection {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(LiSet.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return LiSet.class;
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

	public static class TypeSerializer_Map
			extends ReflectiveSerializer.TypeSerializer {
		@Override
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
			Map map = (Map) graphNode.value;
			DeserState state = (DeserState) ((ArrayIterator) graphNode.iterator).deserializationState;
			Object o = child.value;
			if (state.hasKey) {
				map.put(state.key, o);
			} else {
				state.key = o;
			}
			state.hasKey = !state.hasKey;
		}

		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(Map.class);
		}

		@Override
		public Iterator<GraphNode> readIterator(GraphNode node) {
			DeserState state = new DeserState();
			ArrayIterator itr = new ArrayIterator(node);
			itr.deserializationState = state;
			return itr;
		}

		@Override
		public Object readValue(GraphNode graphNode) {
			return graphNode.typeNode.newInstance();
		}

		@Override
		public Class serializeAs(Class incoming) {
			return LinkedHashMap.class;
		}

		@Override
		public Iterator<ReflectiveSerializer.GraphNode>
				writeIterator(ReflectiveSerializer.GraphNode node) {
			List list = new ArrayList<>();
			((Map<?, ?>) node.value).entrySet().forEach(e -> {
				list.add(e.getKey());
				list.add(e.getValue());
			});
			Iterator iterator = list.iterator();
			return new MappingIterator<>(iterator,
					new ReflectiveSerializer.GraphNodeMappingCollection(node));
		}

		@Override
		public void writeValueOrContainer(ReflectiveSerializer.GraphNode node,
				ReflectiveSerializer.SerialNode serialNode) {
			ReflectiveSerializer.SerialNode container = serialNode
					.createArrayContainer();
			serialNode.write(node, container);
			node.serialNode = container;
		}

		static class DeserState {
			boolean hasKey = false;

			Object key;
		}
	}

	public static class TypeSerializer_MultikeyMap
			extends ReflectiveSerializer.TypeSerializer {
		@Override
		public void childDeserializationComplete(GraphNode graphNode,
				GraphNode child) {
			MultikeyMap map = (MultikeyMap) graphNode.value;
			DeserState state = (DeserState) ((ArrayIterator) graphNode.iterator).deserializationState;
			Object o = child.value;
			if (state.depth == -1) {
				state.depth = (int) o;
				map.setDepth(state.depth);
			} else {
				state.values.add(o);
				if (state.values.size() == state.depth + 1) {
					map.put((Object[]) state.values
							.toArray(new Object[state.values.size()]));
					state.values.clear();
				}
			}
		}

		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(MultikeyMap.class);
		}

		@Override
		public Iterator<GraphNode> readIterator(GraphNode node) {
			DeserState state = new DeserState();
			ArrayIterator itr = new ArrayIterator(node);
			itr.deserializationState = state;
			return itr;
		}

		@Override
		public Object readValue(GraphNode graphNode) {
			return graphNode.typeNode.newInstance();
		}

		@Override
		public Class serializeAs(Class incoming) {
			return UnsortedMultikeyMap.class;
		}

		@Override
		public Iterator<ReflectiveSerializer.GraphNode>
				writeIterator(ReflectiveSerializer.GraphNode node) {
			List list = new ArrayList<>();
			MultikeyMap map = (MultikeyMap) node.value;
			list.add(map.getDepth());
			map.asTuples(map.getDepth()).forEach(row -> {
				list.addAll((List) row);
			});
			Iterator iterator = list.iterator();
			return new MappingIterator<>(iterator,
					new ReflectiveSerializer.GraphNodeMappingCollection(node));
		}

		@Override
		public void writeValueOrContainer(ReflectiveSerializer.GraphNode node,
				ReflectiveSerializer.SerialNode serialNode) {
			ReflectiveSerializer.SerialNode container = serialNode
					.createArrayContainer();
			serialNode.write(node, container);
			node.serialNode = container;
		}

		static class DeserState {
			int depth = -1;

			List values = new ArrayList<>();
		}
	}

	public static class TypeSerializer_Multimap extends TypeSerializer_Map {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(Multimap.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return Multimap.class;
		}
	}

	public static class TypeSerializer_Set extends TypeSerializer_Collection {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(Set.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return LinkedHashSet.class;
		}
	}

	public static class TypeSerializer_SortedMap extends TypeSerializer_Map {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(SortedMap.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return TreeMap.class;
		}
	}

	public static class TypeSerializer_SortedSet
			extends TypeSerializer_Collection {
		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(SortedSet.class);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return TreeSet.class;
		}
	}

	/*
	 * For objects which can be represented as a javascript value
	 */
	public static class TypeSerializer_Value
			extends ReflectiveSerializer.TypeSerializer {
		@Override
		public boolean handlesDeclaredTypeSubclasses() {
			return true;
		}

		@Override
		public List<Class> handlesTypes() {
			return Arrays.asList(String.class, Long.class, Double.class,
					Float.class, Short.class, Byte.class, Integer.class,
					Boolean.class, Character.class, Date.class, String.class,
					long.class, int.class, short.class, char.class, byte.class,
					boolean.class, double.class, float.class, Enum.class,
					Class.class, void.class, Void.class, byte[].class,
					BasePlace.class, UUID.class, ExtensibleEnum.class);
		}

		@Override
		public boolean isReferenceSerializer() {
			return false;
		}

		@Override
		public Iterator<GraphNode> readIterator(GraphNode node) {
			return null;
		}

		@Override
		public Object readValue(GraphNode graphNode) {
			return graphNode.serialNode.readValue(graphNode);
		}

		@Override
		public Class serializeAs(Class incoming) {
			return incoming;
		}

		@Override
		public Iterator<ReflectiveSerializer.GraphNode>
				writeIterator(ReflectiveSerializer.GraphNode node) {
			return null;
		}

		@Override
		public void writeValueOrContainer(ReflectiveSerializer.GraphNode node,
				ReflectiveSerializer.SerialNode serialNode) {
			serialNode.write(node, node.value);
		}
	}

	public static class ValueSerializerBasePlace
			extends ReflectiveSerializer.ValueSerializer<BasePlace> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(BasePlace.class);
		}

		@Override
		public JsonValue toJson(BasePlace object) {
			return Json.create(object.toTokenStringWithoutAppPrefix());
		}

		@Override
		protected BasePlace fromJson(Class clazz, JsonValue value) {
			return (BasePlace) RegistryHistoryMapper.get()
					.getPlaceOrThrow(value.asString());
		}
	}

	public static class ValueSerializerBoolean
			extends ReflectiveSerializer.ValueSerializer<Boolean> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Boolean.class, boolean.class);
		}

		@Override
		public JsonValue toJson(Boolean object) {
			return Json.create(object);
		}

		@Override
		protected Boolean fromJsonBoolean(JsonBoolean value) {
			return value.asBoolean();
		}
	}

	public static class ValueSerializerByte
			extends ReflectiveSerializer.ValueSerializer<Byte> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Byte.class, byte.class);
		}

		@Override
		public JsonValue toJson(Byte object) {
			return Json.create(object);
		}

		@Override
		protected Byte fromJsonNumber(JsonNumber value) {
			return (byte) value.asNumber();
		}
	}

	public static class ValueSerializerByteArray
			extends ReflectiveSerializer.ValueSerializer<byte[]> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(byte[].class);
		}

		@Override
		public JsonValue toJson(byte[] object) {
			return Json.create(Base64.encodeBytes(object));
		}

		@Override
		protected byte[] fromJson(Class clazz, JsonValue value) {
			return Base64.decode(value.asString());
		}
	}

	public static class ValueSerializerClass
			extends ReflectiveSerializer.ValueSerializer<Class> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Class.class);
		}

		@Override
		public JsonValue toJson(Class object) {
			return Json.create(
					ReflectiveSerializer.serializationClass(object).getName());
		}

		@Override
		protected Class fromJson(Class<? extends Class> clazz,
				JsonValue value) {
			return Reflections.forName(value.asString());
		}
	}

	public static class ValueSerializerDate
			extends ReflectiveSerializer.ValueSerializer<Date> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Date.class);
		}

		@Override
		public JsonValue toJson(Date object) {
			return Json.create(String.valueOf(object.getTime()));
		}

		@Override
		protected Date fromJson(Class clazz, JsonValue value) {
			String asString = value.asString();
			return asString.contains(".")
					? new ValueSerializerTimestamp().fromJson(clazz, value)
					: new Date(Long.parseLong(asString));
		}
	}

	public static class ValueSerializerDouble
			extends ReflectiveSerializer.ValueSerializer<Double> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Double.class, double.class);
		}

		@Override
		public JsonValue toJson(Double object) {
			return Json.create(object);
		}

		@Override
		protected Double fromJsonNumber(JsonNumber value) {
			return value.asNumber();
		}
	}

	public static class ValueSerializerEnum
			extends ReflectiveSerializer.ValueSerializer<Enum> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Enum.class);
		}

		@Override
		public JsonValue toJson(Enum object) {
			return Json.create(object.toString());
		}

		@Override
		protected Enum fromJsonString(Class<? extends Enum> clazz,
				JsonString value) {
			return CommonUtils.getEnumValueOrNull(clazz, value.asString());
		}
	}

	public static class ValueSerializerExtensibleEnum
			extends ReflectiveSerializer.ValueSerializer<ExtensibleEnum> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(ExtensibleEnum.class);
		}

		@Override
		public JsonValue toJson(ExtensibleEnum object) {
			return Json.create(object.toString());
		}

		@Override
		protected ExtensibleEnum fromJsonString(
				Class<? extends ExtensibleEnum> clazz, JsonString value) {
			return ExtensibleEnum.valueOf(clazz, value.asString());
		}
	}

	public static class ValueSerializerFloat
			extends ReflectiveSerializer.ValueSerializer<Float> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Float.class, float.class);
		}

		@Override
		public JsonValue toJson(Float object) {
			return Json.create(object);
		}

		@Override
		protected Float fromJsonNumber(JsonNumber value) {
			return (float) value.asNumber();
		}
	}

	public static class ValueSerializerInteger
			extends ReflectiveSerializer.ValueSerializer<Integer> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Integer.class, int.class);
		}

		@Override
		public JsonValue toJson(Integer object) {
			return Json.create(object);
		}

		@Override
		protected Integer fromJsonNumber(JsonNumber value) {
			return (int) value.asNumber();
		}
	}

	public static class ValueSerializerLong
			extends ReflectiveSerializer.ValueSerializer<Long> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Long.class, long.class);
		}

		@Override
		public JsonValue toJson(Long object) {
			return Json.create(object.toString());
		}

		@Override
		protected Long fromJson(Class clazz, JsonValue value) {
			return Long.parseLong(value.asString());
		}
	}

	public static class ValueSerializerShort
			extends ReflectiveSerializer.ValueSerializer<Short> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Short.class, short.class);
		}

		@Override
		public JsonValue toJson(Short object) {
			return Json.create(object);
		}

		@Override
		protected Short fromJsonNumber(JsonNumber value) {
			return (short) value.asNumber();
		}
	}

	public static class ValueSerializerString
			extends ReflectiveSerializer.ValueSerializer<String> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(String.class);
		}

		@Override
		public JsonValue toJson(String object) {
			return Json.create(object);
		}

		@Override
		protected String fromJson(Class<? extends String> clazz,
				JsonValue value) {
			return value.asString();
		}
	}

	public static class ValueSerializerTimestamp
			extends ReflectiveSerializer.ValueSerializer<Timestamp> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(Timestamp.class);
		}

		@Override
		public JsonValue toJson(Timestamp object) {
			return Json.create(
					Ax.format("%s.%s", object.getTime(), object.getNanos()));
		}

		@Override
		protected Timestamp fromJson(Class clazz, JsonValue value) {
			String[] parts = value.asString().split("\\.");
			Timestamp timestamp = new Timestamp(Long.parseLong(parts[0]));
			timestamp.setNanos(Integer.parseInt(parts[1]));
			return timestamp;
		}
	}

	public static class ValueSerializerUUID
			extends ReflectiveSerializer.ValueSerializer<UUID> {
		@Override
		public List<Class> serializesTypes() {
			return Arrays.asList(UUID.class);
		}

		@Override
		public JsonValue toJson(UUID object) {
			return Json.create(object.toString());
		}

		@Override
		protected UUID fromJson(Class<? extends UUID> clazz, JsonValue value) {
			return UUID.fromString(value.asString());
		}
	}

	static class ArrayIterator implements Iterator<GraphNode> {
		int idx = -1;

		boolean consumed = true;

		GraphNode current;

		GraphNode source;

		Object deserializationState;

		ArrayIterator(GraphNode source) {
			this.source = source;
		}

		@Override
		public boolean hasNext() {
			if (consumed) {
				if (idx < source.serialNode.length() - 1) {
					idx++;
					current = new GraphNode(source, null);
					current.serialNode = source.serialNode.getChild(idx);
					consumed = false;
				} else {
					current = null;
				}
			}
			return current != null;
		}

		@Override
		public GraphNode next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			consumed = true;
			return current;
		}
	}

	static class PropertyIterator implements Iterator<GraphNode> {
		int idx = -1;

		boolean consumed = true;

		GraphNode current;

		GraphNode source;

		Object deserializationState;

		String[] keys;

		PropertyIterator(GraphNode source) {
			this.source = source;
			keys = source.serialNode.keys();
		}

		@Override
		public boolean hasNext() {
			if (consumed) {
				if (idx < keys.length - 1) {
					idx++;
					String key = keys[idx];
					PropertyNode propertyNode = source.typeNode
							.propertyNode(key);
					current = new GraphNode(source, propertyNode);
					current.serialNode = source.serialNode.getChild(key);
					consumed = false;
				} else {
					current = null;
				}
			}
			return current != null;
		}

		@Override
		public GraphNode next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			consumed = true;
			return current;
		}
	}
}
