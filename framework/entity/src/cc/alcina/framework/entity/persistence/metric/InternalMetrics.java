package cc.alcina.framework.entity.persistence.metric;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.ResettingCounter;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLockState;
import cc.alcina.framework.entity.persistence.domain.DomainStoreWaitStats;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.entity.util.Shell.Output;

@RegistryLocation(registryPoint = InternalMetrics.class, implementationType = ImplementationType.SINGLETON)
@Registration.Singleton
public class InternalMetrics {
	private static final int PERSIST_PERIOD = 1000;

	private static final int SLICE_PERIOD = 50;

	private static final int MAX_TRACKERS = 200;

	private static boolean DISABLE_OVER_MAX_TRACKERS = false;

	public static InternalMetrics get() {
		return Registry.impl(InternalMetrics.class);
	}

	public static String profilerFolder(Date date) {
		return Ax.format("profiler/%s", CommonUtils
				.formatDate(date, DateStyle.TIMESTAMP).substring(0, 8));
	}

	private Timer timer;

	private ThreadPoolExecutor sliceExecutor;

	private ThreadPoolExecutor profilerExecutor;

	private ThreadPoolExecutor persistExecutor;

	private ThreadMXBean threadMxBean;

	private volatile boolean started;

	ConcurrentHashMap<Object, InternalMetricData> trackers = new ConcurrentHashMap<>();

	private InternalMetricSliceOracle sliceOracle;

	int sliceSkipCount = 0;

	private MemoryMXBean memoryMxBean;

	Logger logger = LoggerFactory.getLogger(getClass());

	AtomicInteger healthNotificationCounter = new AtomicInteger();

	private boolean highFrequencyProfiling;

	int parseGcLogFrom = 0;

	public void changeTrackerContext(Object marker, String context) {
		InternalMetricData metricData = trackers.get(marker);
		if (metricData != null) {
			metricData.updateContext(context);
		}
	}

	public void endTracker(Object markerObject) {
		if (!started || markerObject == null) {
			return;
		}
		InternalMetricData tracker = trackers.get(markerObject);
		if (tracker != null) {
			synchronized (tracker) {
				tracker.endTime = System.currentTimeMillis();
			}
			if (!ResourceUtilities.is("persistEnabled")) {
				trackers.remove(markerObject);
			}
		}
	}

	public boolean isHighFrequencyProfiling() {
		return this.highFrequencyProfiling;
	}

	public boolean isStarted() {
		return this.started;
	}

	public void logBlackBox() {
		if (!isEnabled() && EntityLayerUtils.isTestOrTestServer()) {
			return;
		}
		String message = trackers.values().stream()
				.filter(imd -> !imd.isFinished())
				.map(InternalMetricData::logForBlackBox)
				.collect(Collectors.joining("\n"));
		logger.warn(message);
		ResourceUtilities.write(message, Ax.format("/tmp/imd-blackbox-%s.txt",
				System.currentTimeMillis()));
	}

	public void setHighFrequencyProfiling(boolean highFrequencyProfiling) {
		this.highFrequencyProfiling = highFrequencyProfiling;
	}

	public void startService() {
		if (!isEnabled() && EntityLayerUtils.isTestOrTestServer()) {
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
		profilerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		profilerExecutor.setThreadFactory(
				new NamedThreadFactory("internalMetrics-profiler"));
		profilerExecutor.submit(this::doProfilerLoop);
		timer = new Timer("internal-metrics-timer");
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					if (sliceExecutor.getActiveCount() == 0) {
						sliceExecutor.submit(() -> {
							if (sliceSkipCount < 50) {
								sliceSkipCount = 0;
							}
							try {
								slice();
							} catch (Throwable e) {
								try {
									isEnabled();
								} catch (Exception e1) {
									// webapp finished
									timer.cancel();
									return;
								}
								e.printStackTrace();
							}
						});
					} else {
						logger.info(
								"internal metrics :: sliceExecutor :: skip");
						if (sliceSkipCount++ == 50) {
							long[] allThreadIds = threadMxBean
									.getAllThreadIds();
							ThreadInfo[] threadInfos = threadMxBean
									.getThreadInfo(allThreadIds, true, true);
							Ax.sysLogHigh("High skip count: ");
							logger.warn(Arrays.asList(threadInfos).toString());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, SLICE_PERIOD, SLICE_PERIOD);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					if (persistExecutor.getActiveCount() == 0) {
						persistExecutor.submit(() -> {
							try {
								persist();
							} catch (Throwable e) {
								try {
									isEnabled();
								} catch (Exception e1) {
									// webapp finished
									timer.cancel();
									return;
								}
								e.printStackTrace();
							}
						});
					} else {
						logger.info(
								"internal metrics :: persistExecutor :: skip");
					}
				} catch (Exception e) {
					e.printStackTrace();
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
			String metricName, Supplier<Boolean> trackMetricsEnabled) {
		if (!started) {
			return;
		}
		if (!isEnabled()) {
			return;
		}
		if (!trackMetricsEnabled.get()) {
			return;
		}
		if (ResourceUtilities.is("healthMetricsOnly")
				&& type != InternalMetricTypeAlcina.health) {
			return;
		}
		if (trackers.size() > MAX_TRACKERS) {
			if (DISABLE_OVER_MAX_TRACKERS) {
				Ax.sysLogHigh(
						"Too many trackers - cancelling internal metrics");
				stopService();
				trackers.clear();
				return;
			} else {
				Ax.sysLogHigh(
						"Too many trackers - resetting current internal metric trackers");
				// stopService();
				trackers.clear();
				return;
			}
		}
		InternalMetricData metric = new InternalMetricData(markerObject,
				callContextProvider, System.currentTimeMillis(),
				Thread.currentThread(), type, metricName);
		if (type == InternalMetricTypeAlcina.health) {
			// reuse persistent record (1 per vm) for health metric
			if (lastHealthMetric != null
					&& lastHealthMetric.persistentId != 0) {
				metric.setPersistentId(lastHealthMetric.persistentId);
			}
			lastHealthMetric = metric;
		}
		trackers.put(markerObject, metric);
	}

	private InternalMetricData lastHealthMetric;

	public void stopService() {
		if (started) {
			started = false;
			timer.cancel();
			sliceExecutor.shutdown();
			profilerExecutor.shutdown();
			persistExecutor.shutdown();
		}
	}

	private boolean isEnabled() {
		return ResourceUtilities.is("enabled");
	}

	private boolean shouldSlice(InternalMetricData imd) {
		return sliceOracle.shouldSlice(imd);
	}

	private void slice() {
		if (!isEnabled()) {
			return;
		}
		if (trackers.isEmpty()) {
			return;
		}
		boolean noSliceBecauseNoLongRunningMetrics = sliceOracle
				.noSliceBecauseNoLongRunningMetrics(trackers.values());
		if (noSliceBecauseNoLongRunningMetrics) {
			return;
		}
		long time = System.currentTimeMillis();
		sliceOracle.beforeSlicePass(threadMxBean);
		List<Long> ids = trackers.values().stream()
				.filter(imd -> !imd.isFinished())
				.filter(imd -> shouldSlice(imd)).map(imd -> imd.thread.getId())
				.collect(Collectors.toList());
		Map<Long, InternalMetricData> metricDataByThreadId = trackers.values()
				.stream()
				.collect(AlcinaCollectors.toKeyMap(imd -> imd.thread.getId()));
		long[] idArray = toLong(ids);
		boolean debugMonitors = sliceOracle.shouldCheckDeadlocks();
		String key = "internalmetrics-extthreadinfo";
		// MetricLogging.get().start(key);
		ThreadInfo[] threadInfos = threadMxBean.getThreadInfo(idArray,
				debugMonitors, debugMonitors);
		// MetricLogging.get().end(key);
		Map<Long, ThreadInfo> threadInfoById = Arrays.stream(threadInfos)
				.filter(Objects::nonNull)
				.collect(AlcinaCollectors.toKeyMap(ti -> ti.getThreadId()));
		trackers.values().stream().filter(imd -> !imd.isFinished())
				.filter(imd -> shouldSlice(imd)).forEach(imd -> {
					addSlice(debugMonitors, threadInfoById, imd);
					// }
				});
	}

	private void addSlice(boolean debugMonitors,
			Map<Long, ThreadInfo> threadInfoById, InternalMetricData imd) {
		synchronized (imd) {
			imd.lastSliceTime = System.currentTimeMillis();
			if (imd.type == InternalMetricTypeAlcina.health) {
				if (healthNotificationCounter.incrementAndGet() % 20 == 0) {
				} else {
					// this is an expansive op (get all threads) -
					// so only do 1/sec
					return;
				}
				logger.info("Internal health metrics monitoring:\n\t{}",
						getMemoryStats());
				long[] allIds = threadMxBean.getAllThreadIds();
				ThreadInfo[] threadInfos2 = threadMxBean.getThreadInfo(allIds,
						debugMonitors, debugMonitors);
				imd.threadHistory.clearElements();
				Map<Thread, StackTraceElement[]> allStackTraces = Thread
						.getAllStackTraces();
				for (ThreadInfo threadInfo : threadInfos2) {
					if (threadInfo == null) {
						continue;
					}
					StackTraceElement[] stackTrace = allStackTraces.entrySet()
							.stream()
							.filter(e -> e.getKey().getId() == threadInfo
									.getThreadId())
							.findFirst().map(e -> e.getValue())
							.orElse(new StackTraceElement[0]);
					imd.addSlice(threadInfo, stackTrace, 0, 0,
							DomainStoreLockState.NO_LOCK,
							new DomainStoreWaitStats());
				}
				return;
			}
			try {
				Thread thread = imd.thread;
				ThreadInfo threadInfo = threadInfoById.get(thread.getId());
				if (threadInfo != null) {
					StackTraceElement[] stackTrace = thread.getStackTrace();
					imd.addSlice(threadInfo, stackTrace, 0L, 0L,
							DomainStoreLockState.NO_LOCK,
							new DomainStoreWaitStats());
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	protected synchronized void persist() {
		if (!isEnabled() || !ResourceUtilities.is("persistEnabled")) {
			return;
		}
		LinkedHashMap<InternalMetricData, InternalMetric> toPersist = null;
		List<InternalMetricData> toRemove = trackers.values().stream()
				.filter(imd -> imd.isFinished() && imd.sliceCount() == 0)
				.collect(Collectors.toList());
		boolean persistAllMetrics = ResourceUtilities.is("persistAllMetrics");
		Predicate<InternalMetricData> requiresSliceFilter = persistAllMetrics
				? imd -> true
				: imd -> imd.sliceCount() > 0;
		if (persistAllMetrics) {
			toRemove.clear();
		}
		toPersist = trackers.values().stream().filter(requiresSliceFilter)
				.filter(imd -> imd.isFinished()
						|| imd.lastPersistTime < imd.lastSliceTime)
				.map(imd -> imd.syncCopyForPersist())
				.collect(Collectors.toMap(imd -> imd, imd -> imd.asMetric(),
						AlcinaCollectors.throwingMerger(), LinkedHashMap::new));
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
									imd.setPersistentId(internalMetric.getId());
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

	ResettingCounter allThreadsRefresher = new ResettingCounter(1000);

	boolean nextIsAlloc = false;

	void doProfilerLoop() {
		while (isEnabled() && started) {
			profile();
		}
	}

	public void forceBlackboxPersist(String fatalExceptionString) {
		this.fatalExceptionString = fatalExceptionString;
		try {
			profile();
			persist();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String fatalExceptionString = "";

	private void profile() {
		String profilerPath = ResourceUtilities.get("profilerPath");
		String alloc = "--alloc 100k -t -d 5 -e alloc ";
		String cpu = "-d 5 --cstack no -t -e cpu ";
		try {
			if (ResourceUtilities.is("profilerEnabled")) {
				int frequency = highFrequencyProfiling ? 50 : 200;
				MetricType type = nextIsAlloc
						|| !ResourceUtilities.is("cpuProfilingEnabled")
								? MetricType.alloc
								: MetricType.cpu;
				String params = type == MetricType.alloc ? alloc : cpu;
				String pid = "jps";
				if (Ax.isTest()) {
					String name = ManagementFactory.getRuntimeMXBean()
							.getName();
					pid = name.replaceFirst("(.+)@.+", "$1");
				}
				String cmd = Ax.format("%s %s -i %sus %s", profilerPath, params,
						frequency, pid);
				Output wrapper = new Shell().noLogging().runBashScript(cmd);
				addMetric(type, wrapper.output);
				String runningMetrics = trackers.values().stream()
						.filter(imd -> !imd.isFinished())
						.map(InternalMetricData::logForBlackBox)
						.collect(Collectors.joining("\n"));
				StringBuilder sb = new StringBuilder();
				String transactionDump = "";
				if (highFrequencyProfiling) {
					if (allThreadsRefresher.check()) {
						allStackTraces = Thread.getAllStackTraces();
					}
					if (allStackTraces != null) {
						for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces
								.entrySet()) {
							sb.append(entry.getKey());
							sb.append("\n");
							StackTraceElement[] value = entry.getValue();
							for (StackTraceElement stackTraceElement : value) {
								sb.append("\t");
								sb.append(stackTraceElement);
								sb.append("\n");
							}
						}
					}
					transactionDump = Transactions.stats()
							.describeTransactions();
				}
				String threadDump = sb.toString();
				String state = Ax.format(
						"%s\nTrackers:\n%s\n\nJobs:\n%s\n\nThreads:\n%s\n\nTransactions:\n%s"
								+ "\n\nFatal:\n%s\n",
						ContainerProvider.get().getContainerState(),
						runningMetrics, ContainerProvider.get().getJobsState(),
						threadDump, transactionDump, fatalExceptionString);
				addMetric(MetricType.metrics, state);
				String gcLogFile = "/opt/jboss/gc.log";
				if (new File(gcLogFile).exists()) {
					GCLogParser.Events events = new GCLogParser().parse(
							gcLogFile, parseGcLogFrom,
							ResourceUtilities.getInteger(getClass(),
									"gcEventThresholdMillis"));
					addMetric(MetricType.gc, events.toString());
					parseGcLogFrom = events.end;
				}
				nextIsAlloc = !nextIsAlloc;
			} else {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addMetric(MetricType type, String string) {
		synchronized (blackboxData) {
			blackboxData.metrics.add(type, string);
			List<String> list = blackboxData.metrics.get(type);
			if (list.size() > 5) {
				list.remove(0);
			}
		}
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

	@RegistryLocation(registryPoint = ContainerProvider.class, implementationType = ImplementationType.SINGLETON)
	@Registration.Singleton
	public static class ContainerProvider {
		public static InternalMetrics.ContainerProvider get() {
			return Registry.impl(InternalMetrics.ContainerProvider.class);
		}

		public String getContainerState() {
			return "---";
		}

		public String getJobsState() {
			return "---";
		}
	}

	public interface InternalMetricType {
	}

	public enum InternalMetricTypeAlcina implements InternalMetricType {
		client, service, health, api, servlet, job, remote_invocation
	}

	public enum MetricType {
		alloc, cpu, gc, metrics
	}

	private BlackboxData blackboxData = new BlackboxData();

	private Map<Thread, StackTraceElement[]> allStackTraces;

	public static class BlackboxData {
		public Multimap<MetricType, List<String>> metrics = new Multimap<>();
	}

	String getBlackboxData() {
		return JacksonUtils.serialize(blackboxData);
	}

	public InternalMetric getMetric(long clientInstanceId, String callName) {
		return CommonPersistenceProvider.get().getCommonPersistence()
				.getItemByKeyValueKeyValue(
						PersistentImpl.getImplementation(InternalMetric.class),
						"clientInstanceId", clientInstanceId, "callName",
						callName);
	}
}
