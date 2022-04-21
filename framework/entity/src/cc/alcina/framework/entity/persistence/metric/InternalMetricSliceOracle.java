package cc.alcina.framework.entity.persistence.metric;

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
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
		} else if (imd.type == InternalMetricTypeAlcina.remote_invocation) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.job) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.servlet) {
			return true;
		} else if (imd.type == InternalMetricTypeAlcina.tranche) {
			return true;
		} else {
			Ax.err("Internal metric type not supported: %s", imd.type);
			return false;
		}
	}
}
