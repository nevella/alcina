package cc.alcina.framework.common.client.collections;

public interface PathAccessor {
	boolean hasPropertyKey(Object bean, String path);

	Object getPropertyValue(Object bean, String path);

	void setPropertyValue(Object bean, String path, Object value);
}
