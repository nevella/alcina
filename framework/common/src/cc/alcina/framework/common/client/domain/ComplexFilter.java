package cc.alcina.framework.common.client.domain;

import java.util.Set;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.FilterCost.HasFilterCost;
import cc.alcina.framework.common.client.logic.domain.Entity;

public interface ComplexFilter<E extends Entity> extends HasFilterCost {
	@Override
	FilterCost estimateFilterCost(int entityCount, DomainFilter... filters);

	Stream<E> evaluate(ComplexFilterContext<E> context,
			DomainFilter... filters);

	boolean handles(Class clazz, DomainFilter... filters);

	int topLevelFiltersConsumed();

	public static interface ComplexFilterContext<E extends Entity> {
		public Stream<E> getEntitiesForIds(Set<Long> ids);

		public Stream<E> getIncomingStream();

		public <E2 extends Entity> ComplexFilterContext<E2>
				getOtherEntityFilterContext(Class<E2> otherEntityClass);

		public <E2 extends Entity> DomainLookup<String, E2>
				getStringLookup(Class<E2> clazz, String propertyPath);

		default Stream<E> appendEvaluatedValueFilter(Set<E> values) {
			Stream<E> incomingStream = getIncomingStream();
			if (values == null || values.size() == 0) {
				return Stream.empty();
			} else {
				if (incomingStream == null) {
					return values.stream();
				} else {
					return incomingStream.filter(values::contains);
				}
			}
		}
	}
}
