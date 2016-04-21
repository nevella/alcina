package cc.alcina.framework.common.client.util;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.projection.GraphProjection;

public interface FromJsonRepresentation {
	static public final DateFormat CONVERSION_DATE_FORMAT = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss +0000");

	default void fieldMapping(JSONObject jso) {
		try {
			Field[] fields = new GraphProjection().getFieldsForClass(this);
			for (Field field : fields) {
				String key = field.getName();
				if (jso.has(key)) {
					Object value = jso.get(key);
					if (value instanceof JSONArray) {
						value = jsArrayToList((JSONArray) value,
								Function.identity());
					}
					if (field.getType() == Date.class && value != null) {
						value = CONVERSION_DATE_FORMAT.parse(value.toString());
					}
					field.set(this, value);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <V> List<V> jsArrayToList(JSONArray array,
			Function function) {
		try {
			List<V> list = new ArrayList<>();
			for (int idx = 0; idx < array.length(); idx++) {
				list.add((V) function.apply(array.get(idx)));
			}
			return list;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static <V> Map<String, V> jsMapToMap(JSONObject jMap) {
		try {
			Map<String, V> map = new LinkedHashMap<>();
			Iterator<String> itr = jMap.keys();
			for (; itr.hasNext();) {
				String k = itr.next();
				map.put(k, (V) jMap.get(k));
			}
			return map;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	void fromJson(JSONObject jso);
}