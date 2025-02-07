package cc.alcina.framework.common.client.domain.search;

import java.util.Comparator;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.search.SearchDefinition;

/**
 * Similar to DomainSearcher, but much simpler implementation (since just sed as
 * a filter)
 * 
 * 
 *
 */
public class BindableSearchFilter implements Predicate, Comparator {
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

	@Override
	public boolean test(Object t) {
		return predicate.test(t);
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (def instanceof BindableSearchDefinition) {
			BindableSearchDefinition bsd = (BindableSearchDefinition) def;
			return bsd.getSearchOrders().compare(o1, o2);
		} else {
			return 0;
		}
	}
}
