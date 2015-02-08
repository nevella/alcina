package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CompositeFilter;
import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.projection.CollectionProjectionFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;

public class AlcinaMemCacheQuery {
	private Collection<Long> ids = new LinkedHashSet<Long>();

	private List<CacheFilter> filters = new ArrayList<CacheFilter>();

	private GraphProjectionFieldFilter fieldFilter;

	private GraphProjectionDataFilter dataFilter;

	private boolean raw;

	public AlcinaMemCacheQuery dataFilter(GraphProjectionDataFilter dataFilter) {
		this.dataFilter = dataFilter;
		return this;
	}

	public AlcinaMemCacheQuery filter(CacheFilter filter) {
		if (filter instanceof CompositeCacheFilter) {
			CompositeCacheFilter compositeFilter = (CompositeCacheFilter) filter;
			if (compositeFilter.canFlatten()) {
				for (CacheFilter sub : compositeFilter.getFilters()) {
					this.filters.add(sub);
				}
				return this;
			}
		}
		this.filters.add(filter);
		return this;
	}

	public AlcinaMemCacheQuery() {
	}

	public AlcinaMemCacheQuery filter(String key, Object value) {
		return filter(new CacheFilter(key, value));
	}

	public AlcinaMemCacheQuery filter(String key, Object value,
			FilterOperator operator) {
		return filter(new CacheFilter(key, value, operator));
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz) {
		return CommonUtils.first(list(clazz));
	}

	public GraphProjectionDataFilter getDataFilter() {
		if (dataFilter == null) {
			dataFilter = Registry.impl(CollectionProjectionFilter.class);
		}
		return this.dataFilter;
	}

	public List<CacheFilter> getFilters() {
		return this.filters;
	}

	public Set<Long> getIds() {
		return (this.ids instanceof Set) ? (Set<Long>) this.ids
				: new LinkedHashSet<Long>(this.ids);
	}

	public GraphProjectionFieldFilter getFieldFilter() {
		if (fieldFilter == null) {
			fieldFilter = Registry.impl(PermissibleFieldFilter.class);
		}
		return this.fieldFilter;
	}

	public AlcinaMemCacheQuery id(long id) {
		this.ids = Collections.singleton(id);
		return this;
	}

	public AlcinaMemCacheQuery ids(Collection<Long> ids) {
		this.ids = ids;
		return this;
	}

	public boolean isRaw() {
		return this.raw;
	}

	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		return AlcinaMemCache.get().list(clazz, this);
	}

	public <T extends HasIdAndLocalId> List<T> allRaw(Class<T> clazz) {
		raw = true;
		ids = AlcinaMemCache.get().getIds(clazz);
		return AlcinaMemCache.get().list(clazz, this);
	}

	public <T extends HasIdAndLocalId> Set<T> asSet(Class<T> clazz) {
		return new LinkedHashSet<T>(AlcinaMemCache.get().list(clazz, this));
	}

	public AlcinaMemCacheQuery fieldFilter(
			GraphProjectionFieldFilter fieldFilter) {
		this.fieldFilter = fieldFilter;
		return this;
	}

	public AlcinaMemCacheQuery raw() {
		this.raw = true;
		return this;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("MemCacheQuery:\n%s",
				CommonUtils.join(filters, ",\n"));
	}

	public <T extends HasIdAndLocalId> int count(Class<T> clazz) {
		return raw().list(clazz).size();
	}
}
