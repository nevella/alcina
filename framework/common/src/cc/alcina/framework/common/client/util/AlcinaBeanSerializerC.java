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

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@Reflected
public class AlcinaBeanSerializerC extends AlcinaBeanSerializer {
	IdentityHashMap seenOut = new IdentityHashMap();

	Map seenIn = new LinkedHashMap();

	public AlcinaBeanSerializerC() {
		propertyFieldName = PROPERTIES;
	}

	@Override
	public <T> T deserialize(String jsonString) {
		JSONObject obj = (JSONObject) JSONParser.parseStrict(jsonString);
		return (T) deserializeObject(obj);
	}

	private Object deserializeField(JSONValue jsonValue, Class type) {
		if (jsonValue == null || jsonValue.isNull() != null) {
			return null;
		}
		if (type == Long.class || type == long.class) {
			return Long.valueOf(jsonValue.isString().stringValue());
		}
		if (type == String.class) {
			return jsonValue.isString().stringValue();
		}
		if (type == Date.class || type == Timestamp.class) {
			String s = jsonValue.isString().stringValue();
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
			return Enum.valueOf(type, jsonValue.isString().stringValue());
		}
		if (type == Integer.class || type == int.class) {
			return ((Double) jsonValue.isNumber().doubleValue()).intValue();
		}
		if (type == Double.class || type == double.class) {
			return jsonValue.isNumber().doubleValue();
		}
		if (type == Boolean.class || type == boolean.class) {
			return jsonValue.isBoolean().booleanValue();
		}
		if (type == Class.class) {
			return getClassMaybeAbbreviated(jsonValue.isString().stringValue());
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
			JSONArray array = jsonValue.isArray();
			int size = array.size();
			for (int i = 0; i < size; i++) {
				JSONValue jv = array.get(i);
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
			JSONArray array = jsonValue.isArray();
			int size = array.size();
			for (int i = 0; i < size; i += 2) {
				JSONValue jv = array.get(i);
				JSONValue jv2 = array.get(i + 1);
				m.put(deserializeValue(jv), deserializeValue(jv2));
			}
			return m;
		}
		if (CommonUtils.isOrHasSuperClass(type, BasePlace.class)) {
			return RegistryHistoryMapper.get()
					.getPlaceOrThrow(jsonValue.isString().stringValue());
		}
		return deserializeObject(jsonValue.isObject());
	}

	private Object deserializeObject(JSONObject jsonObj) {
		if (jsonObj == null) {
			return null;
		}
		JSONString cn = (JSONString) jsonObj.get(CLASS_NAME);
		String cns = cn.stringValue();
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
		JSONObject props = (JSONObject) jsonObj
				.get(getPropertyFieldName(jsonObj));
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)) {
			return deserializeField(jsonObj.get(LITERAL), clazz);
		}
		if (jsonObj.containsKey(REF)) {
			return seenIn
					.get((int) ((JSONNumber) jsonObj.get(REF)).doubleValue());
		}
		Object obj = Reflections.newInstance(clazz);
		seenIn.put(seenIn.size(), obj);
		ClassReflector classReflector = Reflections.at(clazz);
		for (String propertyName : props.keySet()) {
			try {
				Property property = classReflector.property(propertyName);
				if (property == null) {
					throw new NoSuchPropertyException(propertyName);
				}
				Class type = property.getType();
				JSONValue jsonValue = props.get(propertyName);
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

	private Object deserializeValue(JSONValue jv) {
		if (jv.isNull() != null) {
			return null;
		} else {
			return deserializeObject((JSONObject) jv);
		}
	}

	private String getPropertyFieldName(JSONObject jsonObj) {
		return jsonObj.containsKey(PROPERTIES_SHORT) ? PROPERTIES_SHORT
				: PROPERTIES;
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
	private JSONValue serializeField(Object value, Class type) {
		if (value == null) {
			return JSONNull.getInstance();
		}
		if (type == Object.class) {
			type = value.getClass();
		}
		if (type == Long.class || type == long.class || type == String.class
				|| type.isEnum() || (type.getSuperclass() != null
						&& type.getSuperclass().isEnum())) {
			return new JSONString(value.toString());
		}
		if (type == Double.class || type == double.class
				|| type == Integer.class || type == int.class) {
			return new JSONNumber(((Number) value).doubleValue());
		}
		if (type == Boolean.class || type == boolean.class) {
			return JSONBoolean.getInstance((Boolean) value);
		}
		if (type == Date.class) {
			return new JSONString(String.valueOf(((Date) value).getTime()));
		}
		if (type == Class.class) {
			return new JSONString(((Class) value).getName());
		}
		if (type == Timestamp.class) {
			return new JSONString(Ax.format("%s,%s",
					String.valueOf(((Timestamp) value).getTime()),
					String.valueOf(((Timestamp) value).getNanos())));
		}
		if (value instanceof Collection) {
			Collection c = (Collection) value;
			JSONArray arr = new JSONArray();
			int i = 0;
			for (Object o : c) {
				arr.set(i++, serializeObject(o));
			}
			return arr;
		}
		if (value instanceof Map) {
			Map m = (Map) value;
			JSONArray arr = new JSONArray();
			int i = 0;
			for (Object o : m.entrySet()) {
				Entry e = (Entry) o;
				arr.set(i++, serializeObject(e.getKey()));
				arr.set(i++, serializeObject(e.getValue()));
			}
			return arr;
		}
		if (value instanceof BasePlace) {
			return new JSONString(((BasePlace) value).toTokenString());
		}
		return serializeObject(value);
	}

	private JSONObject serializeObject(Object object) {
		if (object == null) {
			return null;
		}
		JSONObject jo = new JSONObject();
		Class<? extends Object> type = object.getClass();
		if (!type.isEnum() && type.getSuperclass() != null
				&& type.getSuperclass().isEnum()) {
			type = type.getSuperclass();
		}
		String typeName = type.getName();
		typeName = normaliseReverseAbbreviation(type, typeName);
		jo.put(CLASS_NAME, new JSONString(typeName));
		Class<? extends Object> clazz = object.getClass();
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)
				|| clazz == Class.class) {
			jo.put(LITERAL, serializeField(object, clazz));
			return jo;
		}
		if (seenOut.containsKey(object)) {
			jo.put(REF, new JSONNumber((int) seenOut.get(object)));
			return jo;
		} else {
			seenOut.put(object, seenOut.size());
		}
		ClassReflector<?> classReflector = Reflections.at(clazz);
		Object template = classReflector.templateInstance();
		JSONObject props = new JSONObject();
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
