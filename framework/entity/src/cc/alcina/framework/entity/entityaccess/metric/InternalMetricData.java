package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.ThreadInfo;
import java.util.Date;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLockState;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreWaitStats;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricType;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;
import cc.alcina.framework.entity.logic.EntityLayerUtils;

public class InternalMetricData {
    transient Object markerObject;

    transient Supplier<String> callContextProvider;

    long startTime;

    public transient Thread thread;

    InternalMetric persistent;

    long lastSliceTime;

    long lastPersistTime;

    long persistentId;

    ThreadHistory threadHistory;

    String metricName;

    long endTime;

    InternalMetricType type;

    private String callContext;

    public InternalMetricData() {
    }

    public InternalMetricData(Object markerObject,
            Supplier<String> callContextProvider, long startTime, Thread thread,
            InternalMetricType type, String metricName) {
        this.markerObject = markerObject;
        this.callContextProvider = callContextProvider;
        try {
            // call early, just in case the context changes
            this.callContext = callContextProvider.get();
        } catch (Exception e) {
            this.callContext = SEUtilities.getFullExceptionMessage(e);
        }
        this.startTime = startTime;
        this.thread = thread;
        this.type = type;
        this.metricName = metricName;
        threadHistory = new ThreadHistory();
    }

    public InternalMetric asMetric() {
        if (persistent == null) {
            persistent = CommonPersistenceProvider.get()
                    .getCommonPersistenceExTransaction()
                    .getNewImplementationInstance(InternalMetric.class);
            persistent.setCallName(metricName);
            persistent.setStartTime(new Date(startTime));
            persistent.setHostName(EntityLayerUtils.getLocalHostName());
            persistent.setObfuscatedArgs(callContext);
            persistent.setThreadName(
                    Ax.format("%s:%s", thread.getName(), thread.getId()));
        }
        String lockType = Ax.blankToEmpty(persistent.getLockType());
        if (lockType.length() < 200) {
            if (DomainStore.stores().writableStore().instrumentation()
                    .isLockedByThread(thread)) {
                lockType += "read;";
            }
            if (DomainStore.stores().writableStore().instrumentation()
                    .isWriteLockedByThread(thread)) {
                lockType += "write;";
            }
        }
        persistent.setEndTime(endTime == 0 ? null : new Date(endTime));
        persistent.setLockType(lockType);
        persistent.setSliceCount(threadHistory.getElementCount());
        persistent.setThreadHistory(threadHistory);
        persistent.setUpdateTime(new Date(lastSliceTime));
        return persistent;
    }

    public boolean isFinished() {
        return endTime != 0;
    }

    public String logForBlackBox() {
        return Ax.format(
                "Thread: %s [%s] - Metric: %s - Start: %s" + "\nContext:\n%s",
                thread.getName(), thread.getId(), metricName,
                new Date(startTime), callContext);
    }

    public int sliceCount() {
        return threadHistory.getElementCount();
    }

    public InternalMetricData syncCopyForPersist() {
        synchronized (this) {
            lastPersistTime = System.currentTimeMillis();
            asMetric();
            InternalMetricData copy = KryoUtils.serialClone(this);
            copy.callContextProvider = callContextProvider;
            copy.markerObject = markerObject;
            copy.thread = thread;
            return copy;
        }
    }

    void addSlice(ThreadInfo info, StackTraceElement[] stackTrace,
            long activeDomainStoreLockTime, long domainStoreWaitTime,
            DomainStoreLockState domainStoreLockState,
            DomainStoreWaitStats waitStats) {
        int maxStackLines = type == InternalMetricTypeAlcina.health ? 100 : 300;
        int maxFrames = type == InternalMetricTypeAlcina.health ? 2000 : 50;
        threadHistory.addElement(info, stackTrace, activeDomainStoreLockTime,
                domainStoreWaitTime, domainStoreLockState, maxStackLines,
                maxFrames, waitStats);
    }
}