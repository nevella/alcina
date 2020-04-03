package cc.alcina.framework.entity.util;

import java.util.List;
import java.util.Optional;

public interface PersistentObjectCache<T> {
	default List<String> allPaths() {
		return listChildPaths("");
	}

	default void clear() {
		allPaths().stream().forEach(this::remove);
	}

	T get(String path);

	List<String> listChildPaths(String path);

	default Optional<T> optional(String path) {
		return Optional.<T> ofNullable(get(path));
	}

	void persist(String path, T value);

	void remove(String path);
}
