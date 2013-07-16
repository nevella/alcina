package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.util.GraphProjection.CollectionProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.PermissibleFieldFilter;

public class AlcinaMemCacheQuery {
	private Collection<Long> ids = new LinkedHashSet<Long>();

	private List<CacheFilter> filters = new ArrayList<CacheFilter>();

	private GraphProjectionFilter permissionsFilter = new PermissibleFieldFilter();

	private GraphProjectionFilter dataFilter = new CollectionProjectionFilter();

	private boolean raw;

	public AlcinaMemCacheQuery dataFilter(GraphProjectionFilter dataFilter) {
		this.dataFilter = dataFilter;
		return this;
	}

	public AlcinaMemCacheQuery filter(CacheFilter filter) {
		this.filters.add(filter);
		return this;
	}

	public AlcinaMemCacheQuery filter(String key, Object value) {
		return filter(new CacheFilter(key, value));
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz) {
		return CommonUtils.first(list(clazz));
	}

	public GraphProjectionFilter getDataFilter() {
		return this.dataFilter;
	}

	public List<CacheFilter> getFilters() {
		return this.filters;
	}

	public Set<Long> getIds() {
		return (this.ids instanceof Set) ? (Set<Long>) this.ids
				: new LinkedHashSet<Long>(this.ids);
	}

	public GraphProjectionFilter getPermissionsFilter() {
		return this.permissionsFilter;
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

	public synchronized <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		return AlcinaMemCache.get().list(clazz, this);
	}

	public AlcinaMemCacheQuery permissionsFilter(
			GraphProjectionFilter permissionsFilter) {
		this.permissionsFilter = permissionsFilter;
		return this;
	}

	public AlcinaMemCacheQuery raw() {
		this.raw = true;
		return this;
	}
}
