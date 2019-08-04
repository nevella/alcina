package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;

@RegistryLocation(registryPoint = InternalMetricSliceOracle.class, implementationType = ImplementationType.SINGLETON)
public class InternalMetricSliceOracle {
    private List<Long> deadlockedThreadIds;

    private Map<Thread, Long> activeDomainStoreLockTimes;

    private long sliceOnDomainStoreLockTime = ResourceUtilities.getLong(
            InternalMetricSliceOracle.class, "sliceOnDomainStoreLockTime");

    public void beforeSlicePass(ThreadMXBean threadMxBean) {
        long[] threadIdArray = threadMxBean.findDeadlockedThreads();
        deadlockedThreadIds = CommonUtils.wrapLongArray(threadIdArray);
        activeDomainStoreLockTimes = DomainStore.stores().writableStore()
                .instrumentation().getActiveDomainStoreLockTimes();
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
        } else {
            throw new UnsupportedOperationException();
        }
    }
}