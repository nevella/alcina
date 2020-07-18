package cc.alcina.framework.entity.util;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface PersistentObjectCache<T> {
	public Class<T> getPersistedClass();

	default List<String> allPaths() {
		return listChildPaths("");
	}

	default SingletonCache<T> asSingletonCache() {
		withCreateIfNonExistent(true);
		return new SingletonCache<>(this);
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

	PersistentObjectCache<T>
			withCreateIfNonExistent(boolean createIfNonExistent);

	PersistentObjectCache<T> withRetainInMemory(boolean retainInMemory);

	public interface ClusteredPersistentObjectCacheProvider {
		public static
				PersistentObjectCache.ClusteredPersistentObjectCacheProvider
				get() {
			return Registry.impl(
					PersistentObjectCache.ClusteredPersistentObjectCacheProvider.class);
		}

		<T> PersistentObjectCache<T> createCache(Class<T> clazz);
	}

	public static class SingletonCache<T> {
		private PersistentObjectCache<T> delegate;

		T value = null;

		public SingletonCache(PersistentObjectCache<T> delegate) {
			this.delegate = delegate;
		}

		public void clear() {
			try {
				set(delegate.getPersistedClass().newInstance());
				persist();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public synchronized T get() {
			if (value == null) {
				value = delegate.get(getPath());
			}
			return value;
		}

		public synchronized void persist() {
			Preconditions.checkState(value != null);
			delegate.persist(getPath(), value);
		}

		public synchronized void set(T value) {
			this.value = value;
		}

		private String getPath() {
			return delegate.getPersistedClass().getName();
		}
	}
}
