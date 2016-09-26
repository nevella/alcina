package cc.alcina.framework.common.client.util;

import org.json.JSONObject;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public interface DefaultJsonRepresentation
		extends HasJsonRepresentation, FromJsonRepresentation {
	@Override
	default JSONObject asJson() {
		try {
			return fieldMapping();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	default void fromJson(JSONObject jso) {
		fieldMapping(jso);
	}
	default void fromJson(String json) {
		try {
			fieldMapping(new JSONObject(json));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		
	}
}
