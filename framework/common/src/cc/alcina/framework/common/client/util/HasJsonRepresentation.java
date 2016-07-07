package cc.alcina.framework.common.client.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
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
	JSONObject asJson() throws JSONException;

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

	static Object encode(Object value) {
		if (value instanceof Date) {
			return String.format("__JsDate(%s)", ((Date) value).getTime());
		} else {
			return value;
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

	default JSONArray toJsArray(List<? extends HasJsonRepresentation> objects) {
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

	default JSONObject fieldMapping() {
		return fieldMapping(null);
	}

	default JSONObject fieldMapping(List<String> ignoreFields) {
		try {
			Field[] fields = new GraphProjection().getFieldsForClass(this);
			JSONObject jso = new JSONObject();
			Object templateInstance = getClass().newInstance();
			for (Field field : fields) {
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
}