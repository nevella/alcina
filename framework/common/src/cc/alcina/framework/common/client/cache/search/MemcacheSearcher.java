package cc.alcina.framework.common.client.cache.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.cache.CacheQuery;
import cc.alcina.framework.common.client.cache.CompositeCacheFilter;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class MemcacheSearcher {
	private static UnsortedMultikeyMap<MemcacheCriterionHandler> handlers = new UnsortedMultikeyMap<MemcacheCriterionHandler>(
			2);

	private static Map<Class, MemcacheDefinitionHandler> definitionHandlers = new LinkedHashMap<>();

	private static void setupHandlers() {
		if (handlers.isEmpty()) {
			List<MemcacheCriterionHandler> impls = Registry
					.impls(MemcacheCriterionHandler.class);
			for (MemcacheCriterionHandler handler : impls) {
				handlers.put(handler.handlesSearchDefinition(),
						handler.handlesSearchCriterion(), handler);
			}
			List<MemcacheDefinitionHandler> defImpls = Registry
					.impls(MemcacheDefinitionHandler.class);
			for (MemcacheDefinitionHandler handler : defImpls) {
				definitionHandlers.put(handler.handlesSearchDefinition(),
						handler);
			}
		}
	}

	private SearchDefinition def;

	static class TQuery extends CacheQuery<TQuery> {
		@Override
		public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
			Collection<T> values = Registry.impl(SearcherCollectionSource.class)
					.getCollectionFor(clazz);
			LooseContextInstance snapshot = LooseContext.getContext()
					.snapshot();
			return values.parallelStream().filter(v -> {
				try {
					LooseContext.push();
					LooseContext.putContext(snapshot);
					for (CacheFilter filter : getFilters()) {
						if (!filter.asCollectionFilter().allow(v)) {
							return false;
						}
					}
					return true;
				} finally {
					LooseContext.pop();
				}
			}).collect(Registry.impl(ListCollector.class).toList());
		}
	}

	CacheQuery query = new TQuery();

	public <T extends HasIdAndLocalId> List<T> search(SearchDefinition def,
			Class<T> clazz, Comparator<T> order) {
		this.def = def;
		setupHandlers();
		processDefinitionHandler();
		processHandlers();
		List<T> list = query.list(clazz);
		list.sort(order);
		return list;
	}

	private void processDefinitionHandler() {
		MemcacheDefinitionHandler handler = definitionHandlers
				.get(def.getClass());
		if (handler != null) {
			query.filter(handler.getFilter(def));
		}
	}

	private MemcacheCriterionHandler getCriterionHandler(SearchCriterion sc) {
		return handlers.get(def.getClass(), sc.getClass());
	}

	protected void processHandlers() {
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
						System.err.println(CommonUtils.formatJ(
								"No handler for def/class %s - %s\n",
								def.getClass().getSimpleName(),
								sc.getClass().getSimpleName()));
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
