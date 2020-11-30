package cc.alcina.framework.entity.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.Reflections;
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
		Optional<KeyValuePersistent> kvp = KeyValuePersistent
				.byKey(joinPath(path));
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

	@Override
	public Class<T> getPersistedClass() {
		return clazz;
	}

	@Override
	public List<String> listChildPaths(String path) {
		String parentPath = SEUtilities.getParentPath(joinPath(path));
		return KeyValuePersistent.byParentKey(parentPath).stream()
				.map(kvp -> kvp.getKey().substring(joinPath("").length()))
				.collect(Collectors.toList());
	}

	@Override
	public void persist(String path, T value) {
		KeyValuePersistent.persistObject(joinPath(path), value);
	}

	@Override
	public void remove(String path) {
		KeyValuePersistent.remove(joinPath(path));
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

	protected String joinPath(String path) {
		return Ax.format("%s/%s/%s", getClass().getSimpleName(), base, path);
	}
}
