package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

@SuppressWarnings("unchecked")
public class AlcinaBeanSerializer {
	private static final String PROPERTIES = "props";

	private static final String CLASS_NAME = "cn";

	public <T> T deserialize(String jsonString) {
		JSONObject obj = (JSONObject) JSONParser.parseStrict(jsonString);
		return (T) deserializeObject(obj);
	}

	private Object deserializeObject(JSONObject jsonObj) {
		if (jsonObj == null) {
			return null;
		}
		JSONString cn = (JSONString) jsonObj.get(CLASS_NAME);
		JSONObject props = (JSONObject) jsonObj.get(PROPERTIES);
		Class clazz = CommonLocator.get().classLookup().getClassForName(
				cn.stringValue());
		Object obj = CommonLocator.get().classLookup().newInstance(clazz);
		GwittirBridge gb = GwittirBridge.get();
		for (String propertyName : props.keySet()) {
			Class type = gb.getPropertyType(clazz, propertyName);
			JSONValue jsonValue = props.get(propertyName);
			Object value = deserializeField(jsonValue, type);
			gb.setPropertyValue(obj, propertyName, value);
		}
		return obj;
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
		if (type == Date.class) {
			return new Date(Long.parseLong(jsonValue.isString().stringValue()));
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
		Collection c = null;
		if (type == Set.class || type == LinkedHashSet.class) {
			c = new LinkedHashSet();
		}
		if (type == HashSet.class) {
			c = new HashSet();
		}
		if (type == ArrayList.class) {
			c = new ArrayList();
		}
		if (c != null) {
			JSONArray array = jsonValue.isArray();
			int size = array.size();
			for (int i = 0; i < size; i++) {
				JSONValue jv = array.get(i);
				if (jv.isNull() != null) {
					c.add(null);
				} else {
					c.add(deserializeObject((JSONObject) jv));
				}
			}
			return c;
		}
		return deserializeObject(jsonValue.isObject());
	}

	public String serialize(Object bean) {
		return serializeObject(bean).toString();
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
		if(type==Object.class){
			type=value.getClass();
		}
		if (type == Long.class ||type==long.class|| type == String.class || type.isEnum()) {
			return new JSONString(value.toString());
		}
		if (type==Double.class||type==double.class||type==Integer.class||type==int.class) {
			return new JSONNumber(((Number) value).doubleValue());
		}
		if (type == Boolean.class||type==boolean.class) {
			return JSONBoolean.getInstance((Boolean) value);
		}
		if (type == Date.class) {
			return new JSONString(String.valueOf(((Date) value).getTime()));
		}
		if (value instanceof Map) {
			return null;
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
		return serializeObject(value);
	}

	private JSONObject serializeObject(Object object) {
		if (object == null) {
			return null;
		}
		JSONObject jo = new JSONObject();
		jo.put(CLASS_NAME, new JSONString(object.getClass().getName()));
		GwittirBridge gb = GwittirBridge.get();
		BeanDescriptor descriptor = gb.getDescriptor(object);
		Class<? extends Object> clazz = object.getClass();
		Object template = CommonLocator.get().classLookup()
				.getTemplateInstance(clazz);
		Property[] properties = descriptor.getProperties();
		JSONObject props = new JSONObject();
		jo.put(PROPERTIES, props);
		for (Property property : properties) {
			if (property.getMutatorMethod() == null) {
				continue;
			}
			String name = property.getName();
			if (gb.getAnnotationForProperty(clazz, AlcinaTransient.class, name) != null) {
				continue;
			}
			Object value = gb.getPropertyValue(object, name);
			if (!CommonUtils.equalsWithNullEquality(value, gb.getPropertyValue(
					template, name))) {
				props.put(name, serializeField(value, property.getType()));
			}
		}
		return jo;
	}
}
