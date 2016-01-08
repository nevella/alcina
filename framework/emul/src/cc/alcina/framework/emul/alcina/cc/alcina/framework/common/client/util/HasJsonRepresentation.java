package cc.alcina.framework.common.client.util;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface HasJsonRepresentation {
	
	default JSONObject asJson() throws JSONException {
		return new JSONObject();
	}
	default JSONObject fieldMapping() {
		return new JSONObject();
	}
	default JSONArray toJsArray(List<? extends HasJsonRepresentation> objects) {
		return new JSONArray();
	}
	default JSONObject simpleMapping(Object... params) {
		return new JSONObject();
	}
}