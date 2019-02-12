package cc.alcina.framework.gwt.client.util;

import java.util.Date;

import elemental.json.JsonObject;

public class ElementalJsonWrapper {
	private JsonObject json;

	public ElementalJsonWrapper(JsonObject json) {
		this.json = json;
	}

	public boolean getBoolean(String key) {
		return json.hasKey(key) ? json.getBoolean(key) : false;
	}

	public double getNumber(String key) {
		return json.hasKey(key) ? json.getNumber(key) : 0.0;
	}

	public String getString(String key) {
		return json.hasKey(key) ? json.getString(key) : null;
	}

	public Date parseDate(String key) {
		return json.hasKey(key) ? new Date(Long.parseLong(
				json.getString(key).replaceFirst("__JsDate\\((\\d*)\\)", "$1")))
				: null;
	}
}
