package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface JsonObjectSerializer {
	static JsonObjectSerializer get() {
		return Registry.impl(JsonObjectSerializer.class);
	}

	<T> T deserialize(String json, Class<T> clazz);

	String serialize(Object object);

	default String serializeNoThrow(Object object) {
		try {
			return serialize(object);
		} catch (Exception e) {
			return Ax.blankTo(e.getMessage(), e.getClass().getName());
		}
	}
}
