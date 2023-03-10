package cc.alcina.framework.entity.persistence.metric;

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;

@Registration.Singleton
public class InternalMetricSliceOracle {
	private List<Long> deadlockedThreadIds;

	public void beforeSlicePass(ThreadMXBean threadMxBean) {
		long[] threadIdArray = threadMxBean.findDeadlockedThreads();
		deadlockedThreadIds = CommonUtils.wrapLongArray(threadIdArray);
	}

	public boolean noSliceBecauseNoLongRunningMetrics(
			Collection<InternalMetricData> values) {
		deadlockedThreadIds = new ArrayList<>();
		return values.stream()
				.allMatch(imd -> imd.isFinished() || !shouldSlice(imd));
	}

	public boolean shouldCheckDeadlocks() {
		return deadlockedThreadIds.size() > 0;
	}

	public boolean shouldSlice(InternalMetricData imd) {
		long timeFromStart = System.currentTimeMillis() - imd.startTime;
		long timeSinceLastSlice = System.currentTimeMillis()
				- imd.lastSliceTime;
		if (deadlockedThreadIds.contains(imd.thread.getId())) {
			return true;
		}
		if (imd.type == InternalMetricTypeAlcina.client
				&& imd.type.shouldSlice()) {
			long initialClientDelay = Configuration.key("initialClientDelay")
					.longValue();
			int sliceCount = imd.sliceCount();
			if (sliceCount == 0) {
				return timeFromStart > initialClientDelay;
			} else {
				if (sliceCount < 5) {
					return timeSinceLastSlice > initialClientDelay / 2;
				} else if (sliceCount < 10) {
					return timeSinceLastSlice > initialClientDelay;
				} else {
					return timeSinceLastSlice > initialClientDelay * 2;
				}
			}
		} else {
			return imd.type.shouldSlice();
		}
	}
}
