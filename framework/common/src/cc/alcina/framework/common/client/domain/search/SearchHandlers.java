package cc.alcina.framework.common.client.domain.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.CompositeFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainSearcher.DomainSearcherFilter;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class SearchHandlers {
	// immutable post-setup
	static UnsortedMultikeyMap<DomainCriterionHandler> handlers = new UnsortedMultikeyMap<DomainCriterionHandler>(
			2);

	static Map<Class, DomainDefinitionHandler> definitionHandlers = new LinkedHashMap<>();

	static synchronized void ensureHandlers() {
		Logger logger = LoggerFactory.getLogger(DomainSearcher.class);
		if (handlers.isEmpty()) {
			List<DomainCriterionHandler> impls = Registry
					.query(DomainCriterionHandler.class).implementations()
					.collect(Collectors.toList());
			for (DomainCriterionHandler handler : impls) {
				handlers.put(handler.handlesSearchDefinition(),
						handler.handlesSearchCriterion(), handler);
				logger.debug(
						"registering search criterion handler: \n{} => {} :: {}",
						handler.getClass().getName(),
						handler.handlesSearchDefinition() == null
								? "(null defs)"
								: handler.handlesSearchDefinition()
										.getSimpleName(),
						handler.handlesSearchCriterion().getSimpleName());
			}
			List<DomainDefinitionHandler> defImpls = Registry
					.query(DomainDefinitionHandler.class).implementations()
					.collect(Collectors.toList());
			for (DomainDefinitionHandler handler : defImpls) {
				definitionHandlers.put(handler.handlesSearchDefinition(),
						handler);
				logger.debug(
						"registering search definition handler: \n{} => {} ",
						handler.getClass().getName(),
						handler.handlesSearchDefinition() == null ? "(null)"
								: handler.handlesSearchDefinition()
										.getSimpleName());
			}
		}
	}

	private static DomainCriterionHandler
			getCriterionHandler(SearchDefinition def, SearchCriterion sc) {
		return SearchHandlers.handlers.get(def.getClass(), sc.getClass());
	}

	static void processDefinitionHandler(SearchDefinition def,
			Consumer<DomainFilter> filterConsumer) {
		DomainDefinitionHandler handler = SearchHandlers.definitionHandlers
				.get(def.getClass());
		if (handler != null) {
			filterConsumer.accept(handler.getFilter(def));
		}
	}

	static void processHandlers(SearchDefinition def,
			Consumer<DomainFilter> filterConsumer) {
		Set<CriteriaGroup> criteriaGroups = def.getCriteriaGroups();
		for (CriteriaGroup cg : criteriaGroups) {
			if (!cg.provideIsEmpty()) {
				boolean or = cg.getCombinator() == FilterCombinator.OR;
				CompositeFilter orFilter = new CompositeFilter(or);
				boolean added = false;
				for (SearchCriterion sc : (Set<SearchCriterion>) cg
						.getCriteria()) {
					DomainCriterionHandler handler = getCriterionHandler(def,
							sc);
					if (handler == null) {
						System.err.println(
								Ax.format("No handler for def/class %s - %s\n",
										def.getClass().getSimpleName(),
										sc.getClass().getSimpleName()));
						continue;
					}
					DomainFilter filter = handler.getFilter(sc);
					DomainSearcherFilter searcherFilter = new DomainSearcherFilter(
							filter, sc);
					if (filter != null) {
						if (or) {
							orFilter.add(filter);
						} else {
							filterConsumer.accept(searcherFilter);
						}
						added = true;
					}
				}
				if (added && or) {
					filterConsumer.accept(orFilter);
				}
			}
		}
	}
}
