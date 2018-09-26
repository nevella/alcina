package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;

@RegistryLocation(registryPoint = InternalMetricSliceOracle.class, implementationType = ImplementationType.SINGLETON)
public class InternalMetricSliceOracle {
	private List<Long> deadlockedThreadIds;

	private Map<Thread, Long> activeMemcacheLockTimes;

	private long sliceOnMemcacheLockTime = ResourceUtilities.getLong(
			InternalMetricSliceOracle.class, "sliceOnMemcacheLockTime");

	public void beforeSlicePass(ThreadMXBean threadMxBean) {
		long[] threadIdArray = threadMxBean.findDeadlockedThreads();
		deadlockedThreadIds = CommonUtils.wrapLongArray(threadIdArray);
		activeMemcacheLockTimes = AlcinaMemCache.get().instrumentation()
				.getActiveMemcacheLockTimes();
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
		if (activeMemcacheLockTimes.containsKey(imd.thread)
				&& activeMemcacheLockTimes
						.get(imd.thread) > sliceOnMemcacheLockTime) {
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
		} else {
			throw new UnsupportedOperationException();
		}
	}
}