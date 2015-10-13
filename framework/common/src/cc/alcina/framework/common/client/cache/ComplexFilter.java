package cc.alcina.framework.common.client.cache;

import java.util.Set;

public interface ComplexFilter {
	Set<Long> evaluate(Set<Long> existing, CacheFilter... filters);

	boolean handles(Class clazz, CacheFilter... filters);

	int topLevelFiltersConsumed();
}
