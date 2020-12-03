package cc.alcina.framework.entity.persistence.metric;

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricTypeAlcina;

@RegistryLocation(registryPoint = InternalMetricSliceOracle.class, implementationType = ImplementationType.SINGLETON)
public class InternalMetricSliceOracle {
	private List<Long> deadlockedThreadIds;

	private Map<Thread, Long> activeDomainStoreLockTimes;

	private long sliceOnDomainStoreLockTime = ResourceUtilities.getLong(
			InternalMetricSliceOracle.class, "sliceOnDomainStoreLockTime");

	public void beforeSlicePass(ThreadMXBean threadMxBean) {
		long[] threadIdArray = threadMxBean.findDeadlockedThreads();
		deadlockedThreadIds = CommonUtils.wrapLongArray(threadIdArray);
		activeDomainStoreLockTimes = new LinkedHashMap<>();
	}

	public boolean noSliceBecauseNoLongRunningMetrics(
			Collection<InternalMetricData> values) {
		deadlockedThreadIds = new ArrayList<>();
		activeDomainStoreLockTimes = new LinkedHashMap<>();
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
		if (activeDomainStoreLockTimes.containsKey(imd.thread)
				&& activeDomainStoreLockTimes
						.get(imd.thread) > sliceOnDomainStoreLockTime) {
			return true;
		}
		if (imd.type == InternalMetricTypeAlcina.client) {
			long initialClientDelay = ResourceUtilities.getLong(
					InternalMetricSliceOracle.class, "initialClientDelay");
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
		} else if (imd.type == InternalMetricTypeAlcina.service) {
			return false;
		} else if (imd.type == InternalMetricTypeAlcina.health) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.api) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.api) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.job) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.servlet) {
			return true;
		} else {
			Ax.err("Internal metric type not supported: %s", imd.type);
			return false;
		}
	}
}