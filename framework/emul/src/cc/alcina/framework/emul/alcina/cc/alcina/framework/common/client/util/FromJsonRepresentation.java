package cc.alcina.framework.common.client.util;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gwt.core.shared.GwtIncompatible;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public interface FromJsonRepresentation {
	default void fieldMapping(JSONObject jso) {
		
	}
	void fromJson(JSONObject jso);
}