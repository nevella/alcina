package cc.alcina.framework.common.client.domain;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface ComplexFilter<T extends Entity> {
	Stream<T> evaluate(Stream<T> incoming, DomainFilter... filters);

	boolean handles(Class clazz, DomainFilter... filters);

	int topLevelFiltersConsumed();
}
