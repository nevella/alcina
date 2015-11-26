package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class CacheQuery<Q extends CacheQuery> {
	protected Collection<Long> ids = new LinkedHashSet<Long>();

	private List<CacheFilter> filters = new ArrayList<CacheFilter>();

	protected boolean raw;

	private boolean nonTransactional;

	public CacheQuery() {
	}

	public <T extends HasIdAndLocalId> Set<T> asSet(Class<T> clazz) {
		return new LinkedHashSet<T>(list(clazz));
	}

	public <T extends HasIdAndLocalId> int count(Class<T> clazz) {
		return raw().list(clazz).size();
	}

	public Q filter(CacheFilter filter) {
		if (filter instanceof CompositeCacheFilter) {
			CompositeCacheFilter compositeFilter = (CompositeCacheFilter) filter;
			if (compositeFilter.canFlatten()) {
				for (CacheFilter sub : compositeFilter.getFilters()) {
					this.filters.add(sub);
				}
				return (Q) this;
			}
		}
		this.filters.add(filter);
		return (Q) this;
	}

	public Q filter(String key, Object value) {
		return filter(new CacheFilter(key, value));
	}
	public Q filter(Predicate p) {
		return filter(new CacheFilter(p));
	}

	public Q filter(String key, Object value, FilterOperator operator) {
		return filter(new CacheFilter(key, value, operator));
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz) {
		return CommonUtils.first(list(clazz));
	}

	public <T extends HasIdAndLocalId> T first(Class<T> clazz) {
		return CommonUtils.first(list(clazz));
	}

	public List<CacheFilter> getFilters() {
		return this.filters;
	}

	public Set<Long> getIds() {
		return (this.ids instanceof Set) ? (Set<Long>) this.ids
				: new LinkedHashSet<Long>(this.ids);
	}

	public Q id(long id) {
		this.ids = Collections.singleton(id);
		return (Q) this;
	}

	public Q ids(Collection<Long> ids) {
		this.ids = ids;
		return (Q) this;
	}

	public boolean isNonTransactional() {
		return this.nonTransactional;
	}

	public boolean isRaw() {
		return this.raw;
	}

	public abstract <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) ;

	public Q nonTransactional() {
		this.nonTransactional = true;
		return (Q) this;
	}

	public Q raw() {
		this.raw = true;
		return (Q) this;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("MemCacheQuery:\n%s",
				CommonUtils.join(filters, ",\n"));
	}
}
