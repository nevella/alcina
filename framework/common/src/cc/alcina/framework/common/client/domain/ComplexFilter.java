package cc.alcina.framework.common.client.domain;

import java.util.Set;

public interface ComplexFilter {
	Set<Long> evaluate(Set<Long> existing, DomainFilter... filters);

	boolean handles(Class clazz, DomainFilter... filters);

	int topLevelFiltersConsumed();
}
