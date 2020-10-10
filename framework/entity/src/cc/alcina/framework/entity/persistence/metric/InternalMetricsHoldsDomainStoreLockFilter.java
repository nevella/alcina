package cc.alcina.framework.entity.persistence.metric;

import java.util.function.Predicate;

public class InternalMetricsHoldsDomainStoreLockFilter
		implements Predicate<InternalMetricData> {
	@Override
	public boolean test(InternalMetricData t) {
		return false;
	}
}
