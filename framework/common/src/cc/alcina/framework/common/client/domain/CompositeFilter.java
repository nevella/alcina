package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.List;


public class CompositeFilter extends DomainFilter {
	boolean or = false;

	private List<DomainFilter> filters = new ArrayList<DomainFilter>();

	public CompositeFilter() {
		this(false);
	}

	public CompositeFilter(boolean or) {
		super(null);
		this.or = or;
		this.predicate = new cc.alcina.framework.common.client.collections.CompositeFilter(or);
	}

	public void add(DomainFilter filter) {
		filters.add(filter);
		((cc.alcina.framework.common.client.collections.CompositeFilter) predicate).add(filter.asCollectionFilter());
	}

	@Override
	public boolean canFlatten() {
		if (or && filters.size() > 1) {
			return false;
		}
		for (DomainFilter filter : filters) {
			if (!filter.canFlatten()) {
				return false;
			}
		}
		return true;
	}

	public List<DomainFilter> getFilters() {
		return filters;
	}
}