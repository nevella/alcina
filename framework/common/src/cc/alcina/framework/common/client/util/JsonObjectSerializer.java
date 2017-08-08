package cc.alcina.framework.common.client.util;

public interface JsonObjectSerializer {
	String serialize(Object object);

	<T> T deserialize(String json, Class<T> clazz);
}
