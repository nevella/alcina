package cc.alcina.framework.entity.util;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.SEUtilities;

@RegistryLocation(registryPoint = AlcinaBeanSerializer.class, implementationType = ImplementationType.INSTANCE, priority = 15)
@ClientInstantiable
public class AlcinaBeanSerializerS implements AlcinaBeanSerializer {
	private static final String PROPERTIES = "props";

	private static final String CLASS_NAME = "cn";

	private static final String LITERAL = "lit";

	private ClassLoader cl;

	@Override
	public <T> T deserialize(String jsonString) throws Exception {
		JSONObject obj = new JSONObject(jsonString);
		cl = Thread.currentThread().getContextClassLoader();
		return (T) deserializeObject(obj);
	}

	private Object deserializeObject(JSONObject jsonObj) throws Exception {
		if (jsonObj == null) {
			return null;
		}
		String cn = (String) jsonObj.get(CLASS_NAME);
		Class clazz = cl.loadClass(cn);
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)
				|| clazz == Class.class) {
			return deserializeField(jsonObj.get(LITERAL), clazz);
		}
		JSONObject props = (JSONObject) jsonObj.get(PROPERTIES);
		Object obj = Reflections.classLookup().newInstance(clazz);
		String[] names = JSONObject.getNames(props);
		if (names != null) {
			for (String propertyName : names) {
				Object jsonValue = props.get(propertyName);
				PropertyDescriptor pd = SEUtilities
						.getPropertyDescriptorByName(clazz, propertyName);
				if (pd == null) {
					// ignore (we are graceful...)
				} else {
					Object value2 = deserializeField(jsonValue,
							pd.getPropertyType());
					try {
						SEUtilities.setPropertyValue(obj, propertyName, value2);
					} catch (NoSuchPropertyException e) {
					}
				}
			}
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
		if (type == Class.class) {
			return cl.loadClass(o.toString());
		}
		Collection c = null;
		if (type == Set.class || type == LinkedHashSet.class) {
			c = new LinkedHashSet();
		}
		if (type == HashSet.class) {
			c = new HashSet();
		}
		if (type == ArrayList.class || type == List.class || type==Collection.class) {
			c = new ArrayList();
		}
		if (type == ConcurrentLinkedQueue.class || type == Queue.class) {
			c = new ConcurrentLinkedQueue();
		}
		
		if (c != null) {
			deserializeCollection(o, c);
			return c;
		}
		Map m = null;
		if (type == Multimap.class) {
			return deserializeMultimap(o, new Multimap());
		}
		if (type == Map.class || type == LinkedHashMap.class
				|| type == ConcurrentHashMap.class) {
			m = new LinkedHashMap();
		}
		if (type == HashMap.class) {
			m = new HashMap();
		}
		if (type == CountingMap.class) {
			m = new CountingMap();
		}
		if (m != null) {
			return deserializeMap(o, m);
		}
		return deserializeObject((JSONObject) o);
	}

	protected Object deserializeMap(Object o, Map m) throws JSONException,
			Exception {
		JSONArray array = (JSONArray) o;
		int size = array.length();
		for (int i = 0; i < size; i += 2) {
			JSONObject jv = (JSONObject) array.get(i);
			JSONObject jv2 = (JSONObject) array.get(i + 1);
			m.put(deserializeObject(jv), deserializeObject(jv2));
		}
		return m;
	}

	protected Object deserializeMultimap(Object o, Multimap m)
			throws JSONException, Exception {
		JSONArray array = (JSONArray) o;
		int size = array.length();
		for (int i = 0; i < size; i += 2) {
			JSONObject jv = (JSONObject) array.get(i);
			Object o2 = array.get(i + 1);
			ArrayList c = new ArrayList();
			deserializeCollection(o2, c);
			m.put(deserializeObject(jv), c);
		}
		return m;
	}

	protected void deserializeCollection(Object o, Collection c)
			throws JSONException, Exception {
		if (o instanceof JSONArray) {
			JSONArray array = (JSONArray) o;
			int size = array.length();
			for (int i = 0; i < size; i++) {
				Object jv = array.get(i);
				c.add(deserializeObject((JSONObject) jv));
			}
		}
	}

	@Override
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
		if (type == Long.class
				|| type == long.class
				|| type == String.class
				|| type.isEnum()
				|| (type.getSuperclass() != null && type.getSuperclass()
						.isEnum())) {
			return value.toString();
		}
		if (type == Double.class || type == double.class
				|| type == Integer.class || type == int.class) {
			return (((Number) value).doubleValue());
		}
		if (type == Boolean.class || type == boolean.class) {
			return ((Boolean) value);
		}
		if (type == Class.class) {
			return ((Class) value).getName();
		}
		if (type == Date.class) {
			return (String.valueOf(((Date) value).getTime()));
		}
		if (value instanceof Multimap) {
			Multimap m = (Multimap) value;
			return serializeMultimap(m);
		}
		if (value instanceof Map) {
			Map m = (Map) value;
			return serializeMap(m);
		}
		if (value instanceof Collection) {
			Collection c = (Collection) value;
			return serializeCollection(c);
		}
		return serializeObject(value);
	}

	protected Object serializeCollection(Collection c) throws JSONException,
			Exception {
		JSONArray arr = new JSONArray();
		int i = 0;
		for (Object o : c) {
			arr.put(i++, serializeObject(o));
		}
		return arr;
	}

	protected Object serializeMap(Map m) throws JSONException, Exception {
		JSONArray arr = new JSONArray();
		int i = 0;
		for (Object o : m.entrySet()) {
			Entry e = (Entry) o;
			arr.put(i++, serializeObject(e.getKey()));
			arr.put(i++, serializeObject(e.getValue()));
		}
		return arr;
	}

	protected Object serializeMultimap(Multimap m) throws JSONException,
			Exception {
		JSONArray arr = new JSONArray();
		int i = 0;
		for (Object o : m.entrySet()) {
			Entry e = (Entry) o;
			arr.put(i++, serializeObject(e.getKey()));
			arr.put(i++, serializeCollection((Collection) e.getValue()));
		}
		return arr;
	}

	private JSONObject serializeObject(Object object) throws Exception {
		if (object == null) {
			return null;
		}
		if (object != null
				&& !CommonUtils.isStandardJavaClassOrEnum(object.getClass())) {
			// should implement as a refererer/referee map, otherwise those're
			// invalid cycles
			// if (serialized.containsKey(object)) {
			// throwIfNonZeroFields = true;
			// } else {
			// serialized.put(object, object);
			// }
		}
		JSONObject jo = new JSONObject();
		Class<? extends Object> type = object.getClass();
		if (!type.isEnum() && type.getSuperclass() != null
				&& type.getSuperclass().isEnum()) {
			type = type.getSuperclass();
		}
		jo.put(CLASS_NAME, type.getName());
		Class<? extends Object> clazz = type;
		if (CommonUtils.isStandardJavaClassOrEnum(clazz)
				|| clazz == Class.class) {
			jo.put(LITERAL, serializeField(object, clazz));
			return jo;
		}
		List<PropertyDescriptor> pds = SEUtilities
				.getSortedPropertyDescriptors(clazz);
		JSONObject props = new JSONObject();
		jo.put(PROPERTIES, props);
		Object template = clazz.newInstance();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
				continue;
			}
			String name = pd.getName();
			if (pd.getPropertyType().getAnnotation(AlcinaTransient.class) != null
					|| pd.getReadMethod().getAnnotation(AlcinaTransient.class) != null) {
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
}
