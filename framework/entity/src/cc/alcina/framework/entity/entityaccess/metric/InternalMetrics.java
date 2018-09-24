package cc.alcina.framework.entity.entityaccess.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;

@RegistryLocation(registryPoint = InternalMetrics.class, implementationType = ImplementationType.SINGLETON)
public class InternalMetrics {
	public static InternalMetrics get() {
		return Registry.impl(InternalMetrics.class);
	}

	private Timer timer;

	private int periodMs;

	Map<Object, InternalMetricData> trackers = new LinkedHashMap<>();

	AtomicInteger running = new AtomicInteger();

	public synchronized void end(Object markerObject) {
		if (trackers.get(markerObject) != null) {
			trackers.get(markerObject).endTime = System.currentTimeMillis();
		}
	}

	public synchronized void start(Object markerObject,
			Function<Object, String> logger, int periodMs,
			Predicate<Object> triggerFilter, String metricName) {
		if (periodMs == 0) {
			return;
		}
		trackers.put(markerObject,
				new InternalMetricData(markerObject, logger,
						System.currentTimeMillis(), Thread.currentThread(),
						periodMs, triggerFilter, metricName));
		ensureTimer(100);
	}

	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private synchronized void checkLongRunning() {
		long time = System.currentTimeMillis();
		List<InternalMetricData> toPersist = new ArrayList<>();
		trackers.values().forEach(t -> {
			if (time > t.nextSliceTime && !(t.isFinished() && t.sliceCount == 0)
					&& t.triggerFilter.test(t)) {
				try {
					if (!t.isFinished()) {
						String trace = Arrays.stream(t.thread.getStackTrace())
								.map(Object::toString)
								.collect(Collectors.joining("\n"));
						t.addTrace(trace);
					}
					toPersist.add(t);
					t.generateNextSliceTime();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		});
		trackers.entrySet().removeIf(e -> e.getValue().isFinished());
		if (toPersist.size() > 0) {
			persist(toPersist);
		}
	}

	private void persist(List<InternalMetricData> toPersist) {
		if (running.get() > 0) {
			Ax.out("internal metric - skipping, persistent running");
		}
		new Thread("persist-internal-metric") {
			@Override
			public void run() {
				try {
					running.incrementAndGet();
					Ax.out("persist internal metric: [%s]",
							toPersist.stream().map(imd -> imd.thread.getName())
									.collect(Collectors.joining("; ")));
					CommonPersistenceProvider.get().getCommonPersistence()
							.persistInternalMetrics(toPersist.stream()
									.map(imd -> imd.asMetric())
									.collect(Collectors.toList()));
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					running.decrementAndGet();
				}
			}
		}.start();
	}

	void ensureTimer(int periodMs) {
		if (periodMs == 0 || this.periodMs != periodMs) {
			stop();
			if (periodMs == 0) {
				return;
			} else {
				this.periodMs = periodMs;
				timer = new Timer("internal-metrics-timer");
				timer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						checkLongRunning();
						ensureTimer(100);
					}
				}, periodMs, periodMs);
			}
		}
	}
}
