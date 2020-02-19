package cc.alcina.framework.entity.util;

public interface PersistentObjectCache<T> {
	T get(String name);

	void persist(T value, String name);
}
