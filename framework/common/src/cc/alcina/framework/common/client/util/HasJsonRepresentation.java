package cc.alcina.framework.common.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.projection.GraphProjection;

public interface HasJsonRepresentation {
	static Object encode(Object value) {
		if (value instanceof Date) {
			return String.format("__JsDate(%s)", ((Date) value).getTime());
		} else if (value instanceof HasJsonRepresentation) {
			try {
				return ((HasJsonRepresentation) value).asJson();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} else if (value instanceof List) {
			List list = (List) value;
			Iterator itr = list.iterator();
			if (itr.hasNext()) {
				Object next = itr.next();
				if (next instanceof String) {
					return stringListToJsArray(list);
				} else if (next instanceof HasJsonRepresentation) {
					return toJsArrayStatic(list);
				}
			}
		}
		return value;
	}

	static JSONArray stringListToJsArray(List<String> objects) {
		try {
			JSONArray array = new JSONArray();
			for (String string : objects) {
				array.put(string);
			}
			return array;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static JSONArray
			toJsArrayStatic(List<? extends HasJsonRepresentation> objects) {
		try {
			JSONArray array = new JSONArray();
			for (HasJsonRepresentation object : objects) {
				array.put(object.asJson());
			}
			return array;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static JSONObject toJsMap(Map<String, String> stringMap) {
		try {
			JSONObject result = new JSONObject();
			for (Entry<String, String> entry : stringMap.entrySet()) {
				result.put(entry.getKey(), entry.getValue());
			}
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	JSONObject asJson() throws JSONException;

	default JSONObject fieldMapping() {
		return fieldMapping(null);
	}

	default JSONObject fieldMapping(List<String> ignoreFields) {
		try {
			List<Field> fields = new GraphProjection()
					.getFieldsForClass(this.getClass());
			JSONObject jso = new JSONObject();
			Object templateInstance = getClass().getDeclaredConstructor()
					.newInstance();
			for (Field field : fields) {
				if (Modifier.isTransient(field.getModifiers())) {
					continue;
				}
				String key = field.getName();
				if (ignoreFields != null && ignoreFields.contains(key)) {
					continue;
				}
				Object value = field.get(this);
				if (Objects.equals(value, field.get(templateInstance))) {
				} else {
					jso.put(key, encode(value));
				}
			}
			return jso;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	default JSONObject simpleMapping(Object... params) {
		try {
			JSONObject jso = new JSONObject();
			for (int i = 0; i < params.length; i += 2) {
				String key = (String) params[i];
				Object value = params[i + 1];
				jso.put(key, encode(value));
			}
			return jso;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	default JSONArray toJsArray(List<? extends HasJsonRepresentation> objects) {
		return toJsArrayStatic(objects);
	}
}