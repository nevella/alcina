package cc.alcina.framework.common.client.util;

import com.google.gwt.json.client.JSONObject;

public interface AlcinaBeanSerializerCCustom<T> {
	public T fromJson(JSONObject json);

	public JSONObject toJson(T t);
}
