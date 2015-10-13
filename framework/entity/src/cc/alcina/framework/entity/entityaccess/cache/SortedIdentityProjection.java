package cc.alcina.framework.entity.entityaccess.cache;

import java.util.SortedMap;
import java.util.TreeMap;

import cc.alcina.framework.common.client.cache.CacheProjection;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class SortedIdentityProjection<T extends HasIdAndLocalId> implements
		CacheProjection<T> {
	private Class<T> listenedClass;

	TreeMap<T, T> sorted = new TreeMap<>();

	public SortedMap<T, T> getSorted() {
		return this.sorted;
	}

	private boolean enabled;

	public SortedIdentityProjection(Class<T> listenedClass) {
		this.listenedClass = listenedClass;
	}

	@Override
	public Class<T> getListenedClass() {
		return listenedClass;
	}

	@Override
	public void insert(T o) {
		sorted.put(o, o);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean matches(HasIdAndLocalId h, Object[] keys) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(T o) {
		sorted.remove(o);
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled=enabled;
	}
}