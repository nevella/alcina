package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters.InverseFilter;

public class NotCacheFilter extends CacheFilter {
	private CacheFilter filter;

	public NotCacheFilter(CacheFilter filter) {
		super(null);
		this.filter = filter;
		this.collectionFilter = new InverseFilter(filter.asCollectionFilter());
	}

	@Override
	public boolean canFlatten() {
		return filter.canFlatten();
	}
}