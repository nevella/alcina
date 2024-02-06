package cc.alcina.framework.entity.persistence;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.PersistentObjectCache;

public class KeyValuePersistentCache<T> implements PersistentObjectCache<T> {
	protected String base;

	protected Class<T> clazz;

	protected boolean createIfNonExistent;

	@Override
	public SingletonCache<T> asSingletonCache() {
		createIfNonExistent = true;
		return new SingletonCache<>(this);
	}

	@Override
	public void clear() {
		try {
			LooseContext.pushWithTrue(KeyValuePersistent.CONTEXT_NO_COMMIT);
			PersistentObjectCache.super.clear();
		} finally {
			LooseContext.pop();
		}
		Transaction.commit();
	}

	@Override
	public T get(String path) {
		Optional<KeyValuePersistent> kvp = optionalKvp(path, true);
		if (kvp.isPresent()) {
			return (T) kvp.get().deserializeObject(clazz);
		} else {
			if (createIfNonExistent) {
				T instance = Reflections.newInstance(clazz);
				persist(path, instance);
				return instance;
			}
			return null;
		}
	}

	public String getBase() {
		return this.base;
	}

	@Override
	public Class<T> getPersistedClass() {
		return clazz;
	}

	protected String joinPath(String path) {
		return Ax.format("%s/%s/%s", getClass().getSimpleName(), base, path);
	}

	@Override
	public Optional<Long> lastModified(String path) {
		return optionalKvp(path, false)
				.map(KeyValuePersistent::getLastModificationDate)
				.map(Date::getTime);
	}

	@Override
	public Map<String, Optional<Long>>
			lastModifiedMultiple(List<String> paths) {
		return PersistentObjectCache.super.lastModifiedMultiple(paths);
	}

	@Override
	public List<String> listChildPaths(String path) {
		String parentPath = SEUtilities.getParentPath(joinPath(path));
		return KeyValuePersistent.byParentKey(parentPath).stream()
				.map(kvp -> kvp.getKey().substring(joinPath("").length()))
				.collect(Collectors.toList());
	}

	protected Optional<KeyValuePersistent> optionalKvp(String path,
			boolean populate) {
		Optional<KeyValuePersistent> kvp = KeyValuePersistent
				.byKey(joinPath(path), populate);
		return kvp;
	}

	@Override
	public void persist(String path, T value) {
		persist(path, value, null);
	}

	@Override
	public void persist(String path, T value, CacheMetadata metadata) {
		KeyValuePersistent.persistObject(joinPath(path), value, metadata);
	}

	@Override
	public void remove(String path) {
		KeyValuePersistent.remove(joinPath(path));
	}

	public void setBase(String base) {
		this.base = base;
	}

	public KeyValuePersistentCache<T> withBase(String base) {
		this.base = base;
		return this;
	}

	public KeyValuePersistentCache<T> withClass(Class<T> clazz) {
		this.clazz = clazz;
		return this;
	}

	@Override
	public PersistentObjectCache<T>
			withCreateIfNonExistent(boolean createIfNonExistent) {
		// don't specify (overridden by singleton cache)
		throw new UnsupportedOperationException();
	}

	@Override
	public PersistentObjectCache<T> withRetainInMemory(boolean retainInMemory) {
		// shouldn't be retained
		throw new UnsupportedOperationException();
	}
}
