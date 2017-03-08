package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

public interface JsonObjectSerializer {
	String serialize(Object object);

	<T> T deserialize(String json, Class<T> clazz);
}
