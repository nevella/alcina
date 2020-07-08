package cc.alcina.framework.entity.entityaccess.cache;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.logic.domain.Entity;

public class DomainStoreQuery<V extends Entity> extends DomainQuery<V> {
	private DomainStore store;

	public DomainStoreQuery(Class<V> entityClass, DomainStore store) {
		super(entityClass);
		this.store = store;
	}

	@Override
	public Set<V> asSet() {
		return stream().collect(Collectors.toSet());
	}

	public String getCanonicalPropertyPath(Class clazz, String propertyPath) {
		return store.getCanonicalPropertyPath(clazz, propertyPath);
	}

	public DomainStore getStore() {
		return store;
	}

	@Override
	public List<V> list() {
		return stream().collect(Collectors.toList());
	}

	@Override
	public Stream<V> stream() {
		return store.query(entityClass, this);
	}
}
