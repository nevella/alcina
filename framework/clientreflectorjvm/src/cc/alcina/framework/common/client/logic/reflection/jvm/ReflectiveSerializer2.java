package cc.alcina.framework.common.client.logic.reflection.jvm;

import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

@GwtScriptOnly
/*
 * Note - will need to handle
 * cc.alcina.framework.common.client.logic.domain.UserPropertyPersistable.
 * Support specially
 */
public class ReflectiveSerializer2 {
	private IdentityHashMap serialized = new IdentityHashMap();

	public String writeValueAsString(Map<String, Object> jsonMap)
			throws JsonProcessingException {
		JsonObject json = Json.createObject();
		Set<Entry<String, Object>> entrySet = jsonMap.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			Object value = entry.getValue();
			Class type = value == null ? null : value.getClass();
			String key = entry.getKey();
			JsonValue jsonValue = serializeField(entry.getValue(), type);
			json.put(key, jsonValue);
		}
		return stringify(json);
	}

	public String writeValueAsString(Object genericObject)
			throws JsonProcessingException {
		JsonObject json = (JsonObject) serializeObject(genericObject);
		return stringify(json);
	}

	private native String nativeStringify(JavaScriptObject json) /*-{
    return JSON.stringify(json);
	}-*/;

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
				|| type == Integer.class || type == int.class
				|| type == Short.class || type == short.class
				|| type == Float.class || type == float.class
				|| type == Byte.class || type == byte.class) {
			return Json.create(((Number) value).doubleValue());
		}
		if (type == Boolean.class || type == boolean.class) {
			return Json.create((Boolean) value);
		}
		if (type == Date.class) {
			return Json.create(String.valueOf(((Date) value).getTime()));
		}
		return serializeObject(value);
	}

	private JsonValue serializeObject(Object object) {
		if (object == null) {
			return null;
		}
		JsonObject jo = Json.createObject();
		Class<? extends Object> clazz = object.getClass();
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)) {
			return serializeField(object, clazz);
		}
		if (object instanceof Object[]) {
			Object[] c = (Object[]) object;
			JsonArray arr = Json.createArray();
			int i = 0;
			for (Object o : c) {
				arr.set(i++, serializeObject(o));
			}
			return arr;
		}
		if (object instanceof Collection) {
			Collection c = (Collection) object;
			JsonArray arr = Json.createArray();
			int i = 0;
			for (Object o : c) {
				arr.set(i++, serializeObject(o));
			}
			return arr;
		}
		if (object instanceof Map) {
			Map m = (Map) object;
			JsonObject obj = Json.createObject();
			int i = 0;
			for (Object o : m.entrySet()) {
				Entry e = (Entry) o;
				String key = e.getKey().toString();
				if (key.equals("360 Degree Turn")) {
					int debug = 3;
				}
				obj.put(key, serializeObject(e.getValue()));
			}
			return obj;
		}
		if (clazz.toString().equals("class [Ljava.lang.Double;")) {
			JsonArray arr = Json.createArray();
			Double[] values = (Double[]) object;
			for (int i = 0; i < values.length; i++) {
				Double dVal = values[i];
				arr.set(i, Json.create(dVal));
			}
			return arr;
		}
		if (clazz.toString().equals("class [D")) {
			JsonArray arr = Json.createArray();
			double[] values = (double[]) object;
			for (int i = 0; i < values.length; i++) {
				double dVal = values[i];
				arr.set(i, Json.create(dVal));
			}
			return arr;
		}
		if (serialized.containsKey(object)) {
			return (JsonValue) serialized.get(object);
		}
		GwittirBridge gb = GwittirBridge.get();
		Object template = Reflections.classLookup().getTemplateInstance(clazz);
		BeanDescriptor descriptor = gb.getDescriptor(object);
		Property[] properties = descriptor.getProperties();
		for (Property property : properties) {
			if (property.getMutatorMethod() == null) {
				continue;
			}
			String name = property.getName();
			if (gb.getAnnotationForProperty(clazz, AlcinaTransient.class,
					name) != null) {
				continue;
			}
			Object value = gb.getPropertyValue(object, name);
			jo.put(name, serializeField(value, property.getType()));
		}
		serialized.put(object, jo);
		return jo;
	}

	protected String stringify(JsonObject json) throws JsonProcessingException {
		try {
			if (GWT.isScript()) {
				return nativeStringify((JavaScriptObject) json);
			} else {
				return JsonUtil.stringify(json);
			}
		} catch (Exception e) {
			throw new JsonProcessingException(e);
		}
	}
}
