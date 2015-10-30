package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.collections.CompositeFilter;

public class CompositeCacheFilter extends CacheFilter {
	boolean or = false;

	public CompositeCacheFilter() {
		this(false);
	}

	public CompositeCacheFilter(boolean or) {
		super(null);
		this.or = or;
		this.predicate = new CompositeFilter(or);
	}

	private List<CacheFilter> filters = new ArrayList<CacheFilter>();

	public List<CacheFilter> getFilters() {
		return filters;
	}

	public void add(CacheFilter filter) {
		filters.add(filter);
		((CompositeFilter) predicate).add(filter.asCollectionFilter());
	}

	@Override
	public boolean canFlatten() {
		if (or && filters.size() > 1) {
			return false;
		}
		for (CacheFilter filter : filters) {
			if (!filter.canFlatten()) {
				return false;
			}
		}
		return true;
	}
}