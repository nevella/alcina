package cc.alcina.framework.common.client.util;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.gwt.dev.protobuf.UnknownFieldSet.Field;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.projection.GraphProjection;

public interface HasGwtJsonRepresentation {
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

	static JSONValue encode(Object value) {
		if (value == null) {
			return JSONNull.getInstance();
		}
		
		if (value instanceof Date) {
			return new JSONString(
					Ax.format("__JsDate(%s)", ((Date) value).getTime()));
		} else if (value instanceof HasGwtJsonRepresentation) {
			try {
				return ((HasGwtJsonRepresentation) value).asJson();
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
				} else if (next instanceof HasGwtJsonRepresentation) {
					return toJsArrayStatic(list);
				}
			}
		} else if (value instanceof Boolean) {
			return JSONBoolean.getInstance((Boolean) value);
		} else if (value instanceof Number) {
			return new JSONNumber(((Number) value).doubleValue());
		} else if (value instanceof JSONValue){
			return (JSONValue) value;
		}
		return new JSONString(value.toString());
	}

	static JSONObject toJsMap(Map<String, String> stringMap) {
		try {
			JSONObject result = new JSONObject();
			for (Entry<String, String> entry : stringMap.entrySet()) {
				result.put(entry.getKey(), new JSONString(entry.getValue()));
			}
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	default JSONArray
			toJsArray(List<? extends HasGwtJsonRepresentation> objects) {
		return toJsArrayStatic(objects);
	}

	static JSONArray
			toJsArrayStatic(List<? extends HasGwtJsonRepresentation> objects) {
		try {
			JSONArray array = new JSONArray();
			for (HasGwtJsonRepresentation object : objects) {
				array.set(array.size(), object.asJson());
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
				array.set(array.size(), new JSONString(string));
			}
			return array;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

}