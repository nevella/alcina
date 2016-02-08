package cc.alcina.framework.common.client.util;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gwt.core.shared.GwtIncompatible;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.projection.GraphProjection;

public interface FromJsonRepresentation {
	default void fieldMapping(JSONObject jso) {
		try {
			Field[] fields = new GraphProjection().getFieldsForClass(this);
			for (Field field : fields) {
				String key = field.getName();
				if (jso.has(key)) {
					field.set(this, jso.get(key));
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
	void fromJson(JSONObject jso);
}