package cc.alcina.framework.entity.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.entity.SEUtilities;

@SuppressWarnings("unchecked")
public class AlcinaBeanSerializerS {
	private static final String PROPERTIES = "props";

	private static final String CLASS_NAME = "cn";

	private ClassLoader cl;

	private <T> T deserialize(String jsonString) throws Exception {
		JSONObject obj = new JSONObject(jsonString);
		return (T) deserializeObject(obj);
	}

	private Object deserializeObject(JSONObject jsonObj) throws Exception {
		if (jsonObj == null) {
			return null;
		}
		String cn = (String) jsonObj.get(CLASS_NAME);
		JSONObject props = (JSONObject) jsonObj.get(PROPERTIES);
		Class clazz = cl.loadClass(cn);
		Object obj = CommonLocator.get().classLookup().newInstance(clazz);
		for (String propertyName : Arrays.asList(JSONObject.getNames(props))) {
			Object jsonValue = props.get(propertyName);
			Object value2 = deserializeField(jsonValue, SEUtilities
					.descriptorByName(clazz, propertyName).getPropertyType());
			SEUtilities.setPropertyValue(obj, propertyName, value2);
		}
		return obj;
	}

	private Object deserializeField(Object o, Class type) throws Exception {
		if (o == null || JSONObject.NULL.equals(o)) {
			return null;
		}
		if (type == Long.class || type == long.class) {
			return Long.valueOf(o.toString());
		}
		if (type == String.class) {
			return o.toString();
		}
		if (type == Date.class) {
			return new Date(Long.parseLong(o.toString()));
		}
		if (type.isEnum()) {
			return Enum.valueOf(type, o.toString());
		}
		if (type == Integer.class || type == int.class) {
			return (int) ((Number) o).doubleValue();
		}
		if (type == Double.class || type == double.class) {
			return ((Number) o).doubleValue();
		}
		if (type == Boolean.class || type == boolean.class) {
			return ((Boolean) o).booleanValue();
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
			if (o instanceof JSONArray) {
				JSONArray array = (JSONArray) o;
				int size = array.length();
				for (int i = 0; i < size; i++) {
					Object jv = array.get(i);
					c.add(deserializeObject((JSONObject) jv));
				}
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
			JSONArray array = (JSONArray) o;
			int size = array.length();
			for (int i = 0; i < size; i += 2) {
				JSONObject jv = (JSONObject) array.get(i);
				JSONObject jv2 = (JSONObject) array.get(i + 1);
				m.put(deserializeObject(jv), deserializeObject(jv2));
			}
			return m;
		}
		return deserializeObject((JSONObject) o);
	}

	public String serialize(Object bean) throws Exception {
		return serializeObject(bean).toString();
	}

	/**
	 * Arrays, maps, primitive collections not supported for the mo'
	 * 
	 * @param value
	 * @param type
	 * @return
	 * @throws Exception
	 */
	private Object serializeField(Object value, Class type) throws Exception {
		if (value == null) {
			return JSONObject.NULL;
		}
		if (type == Object.class) {
			type = value.getClass();
		}
		if (type == Long.class || type == long.class || type == String.class
				|| type.isEnum()) {
			return value.toString();
		}
		if (type == Double.class || type == double.class
				|| type == Integer.class || type == int.class) {
			return (((Number) value).doubleValue());
		}
		if (type == Boolean.class || type == boolean.class) {
			return ((Boolean) value);
		}
		if (type == Date.class) {
			return (String.valueOf(((Date) value).getTime()));
		}
		if (value instanceof Map) {
			Map m = (Map) value;
			JSONArray arr = new JSONArray();
			int i = 0;
			for (Object o : m.entrySet()) {
				Entry e=(Entry) o;
				arr.put(i++, serializeObject(e.getKey()));
				arr.put(i++, serializeObject(e.getValue()));
			}
			return arr;
		}
		if (value instanceof Collection) {
			Collection c = (Collection) value;
			JSONArray arr = new JSONArray();
			int i = 0;
			for (Object o : c) {
				arr.put(i++, serializeObject(o));
			}
			return arr;
		}
		return serializeObject(value);
	}

	private JSONObject serializeObject(Object object) throws Exception {
		if (object == null) {
			return null;
		}
		JSONObject jo = new JSONObject();
		jo.put(CLASS_NAME, object.getClass().getName());
		Class<? extends Object> clazz = object.getClass();
		PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
				.getPropertyDescriptors();
		JSONObject props = new JSONObject();
		jo.put(PROPERTIES, props);
		Object template = clazz.newInstance();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() == null) {
				continue;
			}
			String name = pd.getName();
			if (pd.getPropertyType().getAnnotation(AlcinaTransient.class) != null) {
				continue;
			}
			Object value = pd.getReadMethod().invoke(object);
			if (!CommonUtils.equalsWithNullEquality(value, pd.getReadMethod()
					.invoke(template))) {
				props.put(name, serializeField(value, pd.getPropertyType()));
			}
		}
		return jo;
	}

	public <T> T deserialize(String json, ClassLoader cl) throws Exception {
		this.cl = cl;
		return deserialize(json);
	}
}
