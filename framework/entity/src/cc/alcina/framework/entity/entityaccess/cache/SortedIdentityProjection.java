package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public abstract class SortedIdentityProjection<T extends HasIdAndLocalId>
		implements DomainProjection<T> {
	private Class<T> listenedClass;

	TreeMap<T, T> sorted = new TreeMap<>(getComparator());

	private boolean enabled;

	public SortedIdentityProjection(Class<T> listenedClass) {
		this.listenedClass = listenedClass;
	}

	@Override
	public Class<T> getListenedClass() {
		return listenedClass;
	}

	public SortedMap<T, T> getSorted() {
		return this.sorted;
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
		this.enabled = enabled;
	}

	protected abstract Comparator<T> getComparator();
}