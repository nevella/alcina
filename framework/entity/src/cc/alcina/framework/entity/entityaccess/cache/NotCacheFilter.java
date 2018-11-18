package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.collections.CollectionFilters.InverseFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;

public class NotCacheFilter extends DomainFilter {
	private DomainFilter filter;

	public NotCacheFilter(DomainFilter filter) {
		super(null);
		this.filter = filter;
		this.predicate = new InverseFilter(filter.asCollectionFilter());
	}

	@Override
	public boolean canFlatten() {
		return filter.canFlatten();
	}
}