package cc.alcina.framework.common.client.util;

import org.json.JSONException;
import org.json.JSONObject;

public interface DefaultJsonRepresentation extends HasJsonRepresentation,FromJsonRepresentation{
	@Override
	default JSONObject asJson() throws JSONException {
		return fieldMapping();
	}

	@Override
	default void fromJson(JSONObject jso) {
		fieldMapping(jso);
	}

}
