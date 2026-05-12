package cc.alcina.framework.common.client.domain.search;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.domain.DomainFilter;

/**
 * Similar to DomainSearcher, but much simpler implementation (since just sed as
 * a filter)
 * 
 * 
 *
 */
public class BindableSearchFilter implements Predicate, Comparator {
	BindableSearchDefinition def;

	Predicate predicate = o -> true;

	Comparator computedComparator = null;

	public BindableSearchFilter(BindableSearchDefinition def) {
		this.def = def;
		SearchHandlers.ensureHandlers();
		SearchHandlers.processDefinitionHandler(def, this::addFilter);
		SearchHandlers.processHandlers(def, this::addFilter);
		computedComparator = SearchHandlers.computeComparator(def);
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
		if (computedComparator != null) {
			return computedComparator.compare(o1, o2);
		}
		return def.getSearchOrders().compare(o1, o2);
	}

	public <T> T callInContext(Supplier<T> supplier) {
		SearchContext context = null;
		try {
			LooseContext.push();
			context = SearchContext.startContext();
			context.def = def;
			return supplier.get();
		} finally {
			context.end();
			LooseContext.pop();
		}
	}
}
