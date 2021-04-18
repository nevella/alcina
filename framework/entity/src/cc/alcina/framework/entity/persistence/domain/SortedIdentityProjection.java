package cc.alcina.framework.entity.persistence.domain;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.logic.domain.Entity;

public abstract class SortedIdentityProjection<T extends Entity>
		implements DomainProjection<T> {
	private Class<T> listenedClass;

	SortedMap<T, T> sorted = createSortedMap();

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
		if (isIndexable(o)) {
			sorted.put(o, o);
		}
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void remove(T o) {
		if (isIndexable(o)) {
			sorted.remove(o);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	protected SortedMap<T, T> createSortedMap() {
		return new TreeMap<>(getComparator());
	}

	protected abstract Comparator<T> getComparator();

	protected boolean isIndexable(T o) {
		return true;
	}
}