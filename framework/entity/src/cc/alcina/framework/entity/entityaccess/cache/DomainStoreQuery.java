package cc.alcina.framework.entity.entityaccess.cache;

import java.util.List;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;

public class DomainStoreQuery<V extends HasIdAndLocalId>
		extends DomainQuery<V> {
	private GraphProjectionFieldFilter fieldFilter;

	private GraphProjectionDataFilter dataFilter;

	private DomainStore store;

	public DomainStoreQuery(Class<V> clazz, DomainStore store) {
		super(clazz);
		this.store = store;
	}

	public List<V> allRaw() {
		raw = true;
		filterByIds = Domain.ids(clazz);
		return list();
	}

	public DomainStoreQuery<V>
			dataFilter(GraphProjectionDataFilter dataFilter) {
		this.dataFilter = dataFilter;
		return this;
	}

	public DomainStoreQuery<V>
			fieldFilter(GraphProjectionFieldFilter fieldFilter) {
		this.fieldFilter = fieldFilter;
		return this;
	}

	public String getCanonicalPropertyPath(Class clazz, String propertyPath) {
		return store.getCanonicalPropertyPath(clazz, propertyPath);
	}

	public GraphProjectionDataFilter getDataFilter() {
		if (dataFilter == null) {
			dataFilter = Registry.impl(CollectionProjectionFilter.class);
		}
		return this.dataFilter;
	}

	public GraphProjectionFieldFilter getFieldFilter() {
		if (fieldFilter == null) {
			fieldFilter = Registry.impl(PermissibleFieldFilter.class);
		}
		return this.fieldFilter;
	}

	public DomainStore getStore() {
		return store;
	}

	@Override
	public List<V> list() {
		return store.list(clazz, this);
	}
}
