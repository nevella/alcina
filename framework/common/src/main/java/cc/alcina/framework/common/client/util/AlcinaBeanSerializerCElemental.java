package cc.alcina.framework.common.client.util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonValue;

@SuppressWarnings("deprecation")
@Reflected
public class AlcinaBeanSerializerCElemental extends AlcinaBeanSerializer {
	IdentityHashMap seenOut = new IdentityHashMap();

	Map seenIn = new LinkedHashMap();

	public AlcinaBeanSerializerCElemental() {
		propertyFieldName = PROPERTIES;
	}

	@Override
	public <T> T deserialize(String jsonString) {
		JsonObject obj = Json.instance().parse(jsonString);
		return (T) deserializeObject(obj);
	}

	private Object deserializeField(JsonValue jsonValue, Class type) {
		if (jsonValue == null || jsonValue instanceof JsonNull) {
			return null;
		}
		if (type == Long.class || type == long.class) {
			return Long.valueOf(jsonValue.asString());
		}
		if (type == String.class) {
			return jsonValue.asString();
		}
		if (type == Date.class || type == Timestamp.class) {
			String s = jsonValue.asString();
			if (s.contains(",")) {
				String[] parts = s.split(",");
				Timestamp timestamp = new Timestamp(Long.parseLong(parts[0]));
				timestamp.setNanos(Integer.parseInt(parts[1]));
				return timestamp;
			} else {
				if (type == Date.class) {
					return new Date(Long.parseLong(s));
				} else {
					return new Timestamp(Long.parseLong(s));
				}
			}
		}
		if (type.isEnum()) {
			return Enum.valueOf(type, jsonValue.asString());
		}
		if (type == Integer.class || type == int.class) {
			return ((Double) jsonValue.asNumber()).intValue();
		}
		if (type == Double.class || type == double.class) {
			return jsonValue.asNumber();
		}
		if (type == Boolean.class || type == boolean.class) {
			return jsonValue.asBoolean();
		}
		if (type == Class.class) {
			return getClassMaybeAbbreviated(jsonValue.asString());
		}
		Collection c = null;
		if (type == Set.class || type == LinkedHashSet.class) {
			c = new LinkedHashSet();
		}
		if (type == HashSet.class) {
			c = new HashSet();
		}
		if (type == ArrayList.class || type == List.class) {
			c = new ArrayList();
		}
		if (c != null) {
			JsonArray array = (JsonArray) jsonValue;
			int size = array.length();
			for (int i = 0; i < size; i++) {
				JsonValue jv = array.get(i);
				c.add(deserializeValue(jv));
			}
			return c;
		}
		Map m = null;
		if (type == Map.class || type == LinkedHashMap.class) {
			m = new LinkedHashMap();
		}
		if (type == HashMap.class) {
			m = new HashMap();
		}
		if (type == CountingMap.class) {
			m = new CountingMap();
		}
		if (m != null) {
			JsonArray array = (JsonArray) jsonValue;
			int size = array.length();
			for (int i = 0; i < size; i += 2) {
				JsonValue jv = array.get(i);
				JsonValue jv2 = array.get(i + 1);
				m.put(deserializeValue(jv), deserializeValue(jv2));
			}
			return m;
		}
		if (CommonUtils.isOrHasSuperClass(type, BasePlace.class)) {
			RegistryHistoryMapper.get().getPlaceOrThrow(jsonValue.asString());
		}
		return deserializeObject((JsonObject) jsonValue);
	}

	private Object deserializeObject(JsonObject jsonObj) {
		if (jsonObj == null) {
			return null;
		}
		JsonString cn = (JsonString) jsonObj.get(CLASS_NAME);
		String cns = cn.asString();
		Class clazz = null;
		try {
			clazz = getClassMaybeAbbreviated(cns);
		} catch (Exception e1) {
			if (isThrowOnUnrecognisedClass()) {
				throw new RuntimeException(
						Ax.format("class not found - %s", cns));
			} else {
				return null;
			}
		}
		JsonObject props = (JsonObject) jsonObj
				.get(getPropertyFieldName(jsonObj));
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)) {
			return deserializeField(jsonObj.get(LITERAL), clazz);
		}
		if (jsonObj.hasKey(REF)) {
			return seenIn.get((int) ((JsonNumber) jsonObj.get(REF)).asNumber());
		}
		Object obj = Reflections.newInstance(clazz);
		seenIn.put(seenIn.size(), obj);
		ClassReflector classReflector = Reflections.at(clazz);
		for (String propertyName : props.keys()) {
			try {
				Property property = classReflector.property(propertyName);
				if (property == null) {
					throw new NoSuchPropertyException(propertyName);
				}
				Class type = property.getType();
				JsonValue jsonValue = props.get(propertyName);
				Object value = deserializeField(jsonValue, type);
				property.set(obj, value);
			} catch (NoSuchPropertyException e) {
				if (isThrowOnUnrecognisedProperty()) {
					throw new RuntimeException(
							Ax.format("property not found - %s.%s",
									clazz.getSimpleName(), propertyName));
				}
			}
		}
		return obj;
	}

	private Object deserializeValue(JsonValue jv) {
		if (jv instanceof JsonNull) {
			return null;
		} else {
			return deserializeObject((JsonObject) jv);
		}
	}

	private String getPropertyFieldName(JsonObject jsonObj) {
		return jsonObj.hasKey(PROPERTIES_SHORT) ? PROPERTIES_SHORT : PROPERTIES;
	}

	@Override
	public String serialize(Object bean) {
		String string = serializeObject(bean).toString();
		/*
		 * make returned json identical to other (non-ws, normal) json
		 * serializers - that and ordering needed for history token comparisons
		 */
		string = string.replace(", \"", ",\"");
		return string;
	}

	/**
	 * Arrays, maps, primitive collections not supported for the mo'
	 *
	 * @param value
	 * @param type
	 * @return
	 */
	private JsonValue serializeField(Object value, Class type) {
		if (value == null) {
			return Json.createNull();
		}
		if (type == Object.class) {
			type = value.getClass();
		}
		if (type == Long.class || type == long.class || type == String.class
				|| type.isEnum() || (type.getSuperclass() != null
						&& type.getSuperclass().isEnum())) {
			return Json.create(value.toString());
		}
		if (type == Double.class || type == double.class
				|| type == Integer.class || type == int.class) {
			return Json.create(((Number) value).doubleValue());
		}
		if (type == Boolean.class || type == boolean.class) {
			return Json.create((Boolean) value);
		}
		if (type == Date.class) {
			return Json.create(String.valueOf(((Date) value).getTime()));
		}
		if (type == Class.class) {
			return Json.create(((Class) value).getName());
		}
		if (type == Timestamp.class) {
			return Json.create(Ax.format("%s,%s",
					String.valueOf(((Timestamp) value).getTime()),
					String.valueOf(((Timestamp) value).getNanos())));
		}
		if (value instanceof Collection) {
			Collection c = (Collection) value;
			JsonArray arr = Json.createArray();
			int i = 0;
			for (Object o : c) {
				arr.set(i++, serializeObject(o));
			}
			return arr;
		}
		if (value instanceof Map) {
			Map m = (Map) value;
			JsonArray arr = Json.createArray();
			int i = 0;
			for (Object o : m.entrySet()) {
				Entry e = (Entry) o;
				arr.set(i++, serializeObject(e.getKey()));
				arr.set(i++, serializeObject(e.getValue()));
			}
			return arr;
		}
		if (value instanceof BasePlace) {
			return Json.create(((BasePlace) value).toTokenString());
		}
		return serializeObject(value);
	}

	private JsonObject serializeObject(Object object) {
		if (object == null) {
			return null;
		}
		JsonObject jo = Json.createObject();
		Class<? extends Object> type = object.getClass();
		if (!type.isEnum() && type.getSuperclass() != null
				&& type.getSuperclass().isEnum()) {
			type = type.getSuperclass();
		}
		String typeName = type.getName();
		typeName = normaliseReverseAbbreviation(type, typeName);
		jo.put(CLASS_NAME, Json.create(typeName));
		Class<? extends Object> clazz = object.getClass();
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)
				|| clazz == Class.class) {
			jo.put(LITERAL, serializeField(object, clazz));
			return jo;
		}
		if (seenOut.containsKey(object)) {
			jo.put(REF, Json.create((int) seenOut.get(object)));
			return jo;
		} else {
			seenOut.put(object, seenOut.size());
		}
		ClassReflector<?> classReflector = Reflections.at(clazz);
		Object template = classReflector.templateInstance();
		JsonObject props = Json.createObject();
		jo.put(propertyFieldName, props);
		for (Property property : classReflector.properties()) {
			if (property.isReadOnly()) {
				continue;
			}
			String name = property.getName();
			if (property.has(AlcinaTransient.class)) {
				continue;
			}
			Object value = property.get(object);
			if (!CommonUtils.equalsWithNullEquality(value,
					property.get(template))) {
				props.put(name, serializeField(value, property.getType()));
			}
		}
		return jo;
	}
}
