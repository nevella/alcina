package cc.alcina.framework.common.client.util;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gwt.core.shared.GwtIncompatible;

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

	default JSONObject fieldMapping() {
		try {
			Field[] fields = new GraphProjection().getFieldsForClass(this);
			JSONObject jso = new JSONObject();
			for (Field field : fields) {
				String key = field.getName();
				Object value = field.get(this);
				jso.put(key, encode(value));
			}
			return jso;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}