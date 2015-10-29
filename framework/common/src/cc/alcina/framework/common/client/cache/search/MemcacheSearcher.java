package cc.alcina.framework.common.client.cache.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.cache.CacheQuery;
import cc.alcina.framework.common.client.cache.CompositeCacheFilter;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class MemcacheSearcher {
	private static UnsortedMultikeyMap<MemcacheCriterionHandler> handlers = new UnsortedMultikeyMap<MemcacheCriterionHandler>(
			2);

	private static void setupHandlers() {
		if (handlers.isEmpty()) {
			List<MemcacheCriterionHandler> impls = Registry
					.impls(MemcacheCriterionHandler.class);
			for (MemcacheCriterionHandler handler : impls) {
				handlers.put(handler.handlesSearchDefinition(),
						handler.handlesSearchCriterion(), handler);
			}
		}
	}

	private SearchDefinition def;

	static class TQuery extends CacheQuery<TQuery> {
		@Override
		public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
			List<T> result = new ArrayList<>();
			Collection<T> values = Registry
					.impl(SearcherCollectionSource.class).getCollectionFor(
							clazz);
			for (T value : values) {
				boolean allow = true;
				for (CacheFilter filter : getFilters()) {
					if (!filter.asCollectionFilter().allow(value)) {
						allow = false;
						break;
					}
				}
				if (allow) {
					result.add((T) value);
				}
			}
			return result;
		}
	}

	CacheQuery query = new TQuery();

	public <T extends HasIdAndLocalId> List<T> search(SearchDefinition def,
			Class<T> clazz, Comparator<T> order) {
		this.def = def;
		setupHandlers();
		processHandlers(def);
		List<T> list = query.list(clazz);
		list.sort(order);
		return list;
	}

	private MemcacheCriterionHandler getCriterionHandler(SearchCriterion sc) {
		return handlers.get(def.getClass(), sc.getClass());
	}

	protected void processHandlers(SearchDefinition def) {
		Set<CriteriaGroup> criteriaGroups = def.getCriteriaGroups();
		for (CriteriaGroup cg : criteriaGroups) {
			if (!cg.provideIsEmpty()) {
				CompositeCacheFilter cgFilter = new CompositeCacheFilter(
						cg.getCombinator() == FilterCombinator.OR);
				boolean added = false;
				for (SearchCriterion sc : (Set<SearchCriterion>) cg
						.getCriteria()) {
					MemcacheCriterionHandler handler = getCriterionHandler(sc);
					if (handler == null) {
						System.err.println("No handler for class "
								+ sc.getClass());
						continue;
					}
					CacheFilter filter = handler.getFilter(sc);
					if (filter != null) {
						cgFilter.add(filter);
						added = true;
					}
				}
				if (added) {
					query.filter(cgFilter);
				}
			}
		}
	}
}
