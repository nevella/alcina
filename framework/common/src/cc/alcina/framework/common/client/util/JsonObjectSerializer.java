package cc.alcina.framework.common.client.util;

public interface JsonObjectSerializer {
	<T> T deserialize(String json, Class<T> clazz);

	String serialize(Object object);
}
