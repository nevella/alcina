package cc.alcina.framework.common.client.util;

import com.google.gwt.json.client.JSONObject;

public interface AlcinaBeanSerializerCCustom<T> {
	public JSONObject toJson(T t);
	
	public T fromJson(JSONObject json);
}
