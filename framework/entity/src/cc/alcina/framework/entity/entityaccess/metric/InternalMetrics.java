package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.J8Utils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.NamedThreadFactory;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLockState;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreThreads.DomainStoreInstrumentation;

@RegistryLocation(registryPoint = InternalMetrics.class, implementationType = ImplementationType.SINGLETON)
public class InternalMetrics {
    private static final int PERSIST_PERIOD = 1000;

    private static final int SLICE_PERIOD = 50;

    private static final int MAX_TRACKERS = 200;

    private static final boolean DISABLE_OVER_MAX_TRACKERS = true;

    public static InternalMetrics get() {
        return Registry.impl(InternalMetrics.class);
    }

    private Timer timer;

    private ThreadPoolExecutor sliceExecutor;

    private ThreadPoolExecutor persistExecutor;

    private ThreadMXBean threadMxBean;

    private volatile boolean started;

    ConcurrentHashMap<Object, InternalMetricData> trackers = new ConcurrentHashMap<>();

    private InternalMetricSliceOracle sliceOracle;

    int sliceSkipCount = 0;

    private MemoryMXBean memoryMxBean;

    Logger logger = LoggerFactory.getLogger(getClass());

    public void endTracker(Object markerObject) {
        if (!started || markerObject == null) {
            return;
        }
        InternalMetricData tracker = trackers.get(markerObject);
        if (tracker != null) {
            synchronized (tracker) {
                tracker.endTime = System.currentTimeMillis();
            }
        }
    }

    public boolean isStarted() {
        return this.started;
    }

    public void logBlackBox() {
        String message = trackers.values().stream()
                .filter(imd -> !imd.isFinished())
                .map(InternalMetricData::logForBlackBox)
                .collect(Collectors.joining("\n"));
        logger.warn(message);
        ResourceUtilities.write(message, Ax.format("/tmp/imd-blackbox-%s.txt",
                System.currentTimeMillis()));
    }

    public void startService() {
        if (!ResourceUtilities.is(InternalMetrics.class, "enabled")) {
            return;
        }
        this.sliceOracle = Registry.impl(InternalMetricSliceOracle.class);
        Preconditions.checkState(!started);
        started = true;
        sliceExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        sliceExecutor.setThreadFactory(
                new NamedThreadFactory("internalMetrics-slice"));
        persistExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        persistExecutor.setThreadFactory(
                new NamedThreadFactory("internalMetrics-persist"));
        timer = new Timer("internal-metrics-timer");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (sliceExecutor.getActiveCount() == 0) {
                    sliceExecutor.submit(() -> {
                        if (sliceSkipCount < 50) {
                            sliceSkipCount = 0;
                        }
                        try {
                            slice();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    logger.info("internal metrics :: sliceExecutor :: skip");
                    if (sliceSkipCount++ == 50) {
                        long[] allThreadIds = threadMxBean.getAllThreadIds();
                        ThreadInfo[] threadInfos = threadMxBean
                                .getThreadInfo(allThreadIds, true, true);
                        Ax.sysLogHigh("High skip count: ");
                        logger.warn(Arrays.asList(threadInfos).toString());
                    }
                }
            }
        }, SLICE_PERIOD, SLICE_PERIOD);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (persistExecutor.getActiveCount() == 0) {
                    persistExecutor.submit(() -> {
                        try {
                            persist();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    logger.info("internal metrics :: persistExecutor :: skip");
                }
            }
        }, PERSIST_PERIOD, PERSIST_PERIOD);
        threadMxBean = ManagementFactory.getThreadMXBean();
        memoryMxBean = ManagementFactory.getMemoryMXBean();
        threadMxBean.setThreadContentionMonitoringEnabled(true);
        threadMxBean.setThreadCpuTimeEnabled(true);
    }

    public void startTracker(Object markerObject,
            Supplier<String> callContextProvider, InternalMetricType type,
            String metricName) {
        if (!started) {
            return;
        }
        if (trackers.size() > MAX_TRACKERS && DISABLE_OVER_MAX_TRACKERS) {
            Ax.sysLogHigh("Too many trackers - cancelling internal metrics");
            stopService();
            trackers.clear();
            return;
        }
        trackers.put(markerObject,
                new InternalMetricData(markerObject, callContextProvider,
                        System.currentTimeMillis(), Thread.currentThread(),
                        type, metricName));
    }

    public void stopService() {
        if (started) {
            started = false;
            timer.cancel();
            sliceExecutor.shutdown();
            persistExecutor.shutdown();
        }
    }

    private boolean shouldSlice(InternalMetricData imd) {
        return sliceOracle.shouldSlice(imd);
    }

    private void slice() {
        if (trackers.isEmpty()) {
            return;
        }
        long time = System.currentTimeMillis();
        sliceOracle.beforeSlicePass(threadMxBean);
        List<Long> ids = trackers.values().stream()
                .filter(imd -> !imd.isFinished())
                .filter(imd -> shouldSlice(imd)).map(imd -> imd.thread.getId())
                .collect(Collectors.toList());
        long[] idArray = toLong(ids);
        boolean debugMonitors = sliceOracle.shouldCheckDeadlocks();
        String key = "internalmetrics-extthreadinfo";
        // MetricLogging.get().start(key);
        ThreadInfo[] threadInfos = threadMxBean.getThreadInfo(idArray,
                debugMonitors, debugMonitors);
        // MetricLogging.get().end(key);
        Map<Long, ThreadInfo> threadInfoById = Arrays.stream(threadInfos)
                .collect(J8Utils.toKeyMap(ti -> ti.getThreadId()));
        trackers.values().stream().filter(imd -> !imd.isFinished())
                .filter(imd -> shouldSlice(imd)).forEach(imd -> {
                    synchronized (imd) {
                        imd.lastSliceTime = System.currentTimeMillis();
                        if (imd.type == InternalMetricTypeAlcina.health) {
                            logger.info(
                                    "Internal health metrics monitoring:\n\t%s",
                                    getMemoryStats());
                            long[] allIds = threadMxBean.getAllThreadIds();
                            ThreadInfo[] threadInfos2 = threadMxBean
                                    .getThreadInfo(allIds, debugMonitors,
                                            debugMonitors);
                            imd.threadHistory.clearElements();
                            Map<Thread, StackTraceElement[]> allStackTraces = Thread
                                    .getAllStackTraces();
                            for (ThreadInfo threadInfo : threadInfos2) {
                                if (threadInfo == null) {
                                    continue;
                                }
                                StackTraceElement[] stackTrace = allStackTraces
                                        .entrySet().stream()
                                        .filter(e -> e.getKey()
                                                .getId() == threadInfo
                                                        .getThreadId())
                                        .findFirst().map(e -> e.getValue())
                                        .orElse(new StackTraceElement[0]);
                                imd.addSlice(threadInfo, stackTrace, 0, 0,
                                        DomainStoreLockState.NO_LOCK);
                            }
                            return;
                        }
                        try {
                            Thread thread = imd.thread;
                            ThreadInfo threadInfo = threadInfoById
                                    .get(thread.getId());
                            if (threadInfo != null) {
                                StackTraceElement[] stackTrace = thread
                                        .getStackTrace();
                                DomainStoreInstrumentation instrumentation = DomainStore
                                        .stores().writableStore()
                                        .instrumentation();
                                long activeDomainStoreLockTime = instrumentation
                                        .getActiveDomainStoreLockTime(thread);
                                long domainStoreWaitTime = instrumentation
                                        .getDomainStoreWaitTime(thread);
                                DomainStoreLockState domainStoreState = instrumentation
                                        .getDomainStoreLockState(thread);
                                imd.addSlice(threadInfo, stackTrace,
                                        activeDomainStoreLockTime,
                                        domainStoreWaitTime, domainStoreState);
                            }
                        } catch (Exception e) {
                            throw new WrappedRuntimeException(e);
                        }
                    }
                    // }
                });
    }

    protected void persist() {
        LinkedHashMap<InternalMetricData, InternalMetric> toPersist = null;
        List<InternalMetricData> toRemove = trackers.values().stream()
                .filter(imd -> imd.isFinished() && imd.sliceCount() == 0)
                .collect(Collectors.toList());
        toPersist = trackers.values().stream()
                .filter(imd -> imd.sliceCount() > 0)
                .filter(imd -> imd.isFinished()
                        || imd.lastPersistTime < imd.lastSliceTime)
                .map(imd -> imd.syncCopyForPersist())
                .collect(Collectors.toMap(imd -> imd, imd -> imd.asMetric(),
                        J8Utils.throwingMerger(), LinkedHashMap::new));
        if (toPersist.size() > 0) {
            logger.debug("persist internal metric: [%s]",
                    toPersist.keySet().stream().map(imd -> imd.thread.getName())
                            .collect(Collectors.joining("; ")));
            List<InternalMetric> toPersistList = toPersist.values().stream()
                    .collect(Collectors.toList());
            CommonPersistenceProvider.get().getCommonPersistence()
                    .persistInternalMetrics(toPersistList);
            for (InternalMetric internalMetric : toPersistList) {
                InternalMetricData owner = toPersist.entrySet().stream()
                        .filter(e -> e.getValue() == internalMetric).findFirst()
                        .get().getKey();
                trackers.values().stream()
                        .filter(imd -> imd.thread == owner.thread).findFirst()
                        .ifPresent(imd -> {
                            synchronized (imd) {
                                if (imd.persistent != null
                                        && imd.persistent.getId() == 0) {
                                    imd.persistent
                                            .setId(internalMetric.getId());
                                }
                            }
                        });
                if (owner.isFinished()) {
                    logger.info(
                            "removing after finished/persist: {} {} : id {}",
                            owner.metricName, owner.thread,
                            owner.thread.getId());
                    trackers.entrySet()
                            .removeIf(e -> e.getValue().thread == owner.thread);
                }
            }
        }
        trackers.entrySet().removeIf(e -> toRemove.contains(e.getValue()));
    }

    String getMemoryStats() {
        try {
            return String.format("Heap used: %.2fmb\t\tHeap max: %.2fmb",
                    ((double) memoryMxBean.getHeapMemoryUsage().getUsed())
                            / 1000000,
                    ((double) memoryMxBean.getHeapMemoryUsage().getMax())
                            / 1000000);
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception";
        }
    }

    long[] toLong(Collection<Long> list) {
        long[] returnLong = new long[list.size()];
        int iValue = 0;
        for (final Long value : list) {
            if (value == null) {
                returnLong[iValue++] = -1;
            } else {
                returnLong[iValue++] = value;
            }
        }
        return returnLong;
    }

    public interface InternalMetricType {
    }

    public enum InternalMetricTypeAlcina implements InternalMetricType {
        client, service, health;
    }
}
