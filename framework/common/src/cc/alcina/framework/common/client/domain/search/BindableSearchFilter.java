package cc.alcina.framework.common.client.domain.search;

import java.util.function.Predicate;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.search.SearchDefinition;

/**
 * Similar to DomainSearcher, but much simpler implementation (since just sed as
 * a filter)
 * 
 * @author nick@alcina.cc
 *
 */
public class BindableSearchFilter {
	SearchDefinition def;

	Predicate predicate = o -> true;

	public BindableSearchFilter(SearchDefinition def) {
		this.def = def;
		SearchHandlers.ensureHandlers();
		SearchHandlers.processDefinitionHandler(def, this::addFilter);
		SearchHandlers.processHandlers(def, this::addFilter);
	}

	void addFilter(DomainFilter filter) {
		predicate = predicate.and(filter.asPredicate());
	}

	public boolean exclude(Object o) {
		return !predicate.test(o);
	}
}
