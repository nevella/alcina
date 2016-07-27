package cc.alcina.framework.servlet.servlet;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class MetricTracker<T> extends TimerTask {
	private Timer timer;

	private int periodMs;

	Map<T, MetricTrackerStruct> trackers = new LinkedHashMap<>();

	public synchronized void end(T markerObject) {
		trackers.remove(markerObject);
	}

	@Override
	public void run() {
		checkLongRunning();
	}

	public synchronized void start(T markerObject, Function<T, String> logger,
			int periodMs) {
		ensureTimer(periodMs);
		if (periodMs == 0) {
			return;
		}
		trackers.put(markerObject, new MetricTrackerStruct<>(markerObject,
				logger, System.currentTimeMillis()));
	}

	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private synchronized void checkLongRunning() {
		long time = System.currentTimeMillis();
		trackers.values().forEach(t -> {
			long elapsed = time - t.startTime;
			if (elapsed > periodMs) {
				Thread currentThread = Thread.currentThread();
				String trace = Arrays.stream(currentThread.getStackTrace())
						.map(Object::toString)
						.collect(Collectors.joining("\n"));
				try {
					FileWriter fw = new FileWriter(
							"/tmp/alcina-metric-tracker.txt", true);
					String message = String.format(
							"Long-running call:\n\ttid: %s\n\tstart: %s\n\t"
									+ "elapsed: %s\n\tcall: %s\nStack: %s\n\n",
							currentThread.getId(), t.startTime, elapsed,
							t.logger.apply(t.markerObject), trace);
					fw.write(message);
					System.out.println(message);
					fw.close();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		});
	}

	void ensureTimer(int periodMs) {
		if (periodMs == 0 || this.periodMs != periodMs) {
			stop();
			if (periodMs == 0) {
				return;
			} else {
				this.periodMs = periodMs;
				timer = new Timer();
				timer.scheduleAtFixedRate(this, periodMs, periodMs);
			}
		}
	}

	private static class MetricTrackerStruct<T> {
		T markerObject;

		Function<T, String> logger;

		long startTime;

		public MetricTrackerStruct(T markerObject, Function<T, String> logger,
				long startTime) {
			this.markerObject = markerObject;
			this.logger = logger;
			this.startTime = startTime;
		}
	}
}
