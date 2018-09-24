package cc.alcina.framework.entity.entityaccess.metric;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache;
import cc.alcina.framework.entity.logic.EntityLayerUtils;

public class InternalMetricData {
	Object markerObject;

	Function<Object, String> markerObjectArgsFunction;

	long startTime;

	public Thread thread;

	InternalMetric persistent;

	long initialPeriodMs;

	int sliceCount = 0;

	int sliceTimeCount = 0;

	long nextSliceTime;

	Predicate<Object> triggerFilter;

	long persistentId;

	String tmpTrace = "";

	String metricName;

	long endTime;

	public InternalMetricData(Object markerObject,
			Function<Object, String> markerObjectArgsFunction, long startTime,
			Thread thread, int initialPeriodMs, Predicate<Object> triggerFilter,
			String metricName) {
		this.markerObject = markerObject;
		this.markerObjectArgsFunction = markerObjectArgsFunction;
		this.startTime = startTime;
		this.thread = thread;
		this.initialPeriodMs = initialPeriodMs;
		this.triggerFilter = triggerFilter;
		this.metricName = metricName;
		this.generateNextSliceTime();
	}

	public void addTrace(String trace) {
		tmpTrace += Ax.format("Slice time:\n%s\nTrace:\n%s\n",
				LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
				trace);
		sliceCount++;
	}

	public InternalMetric asMetric() {
		if (persistent == null) {
			persistent = CommonPersistenceProvider.get()
					.getCommonPersistenceExTransaction()
					.getNewImplementationInstance(InternalMetric.class);
			persistent.setCallName(metricName);
			persistent.setStartTime(new Date(startTime));
			persistent.setHostName(EntityLayerUtils.getLocalHostName());
			persistent.setObfuscatedArgs(
					markerObjectArgsFunction.apply(markerObject));
			persistent.setThreadName(thread.getName());
		}
		String lockType = null;
		if (AlcinaMemCache.get().instrumentation().isLockedByThread(thread)) {
			lockType = "read";
		}
		if (AlcinaMemCache.get().instrumentation()
				.isWriteLockedByThread(thread)) {
			lockType = "write";
		}
		persistent.setEndTime(endTime == 0 ? null : new Date(endTime));
		persistent.setLockType(lockType);
		persistent.setSliceCount(sliceCount);
		persistent.setSliceJson(tmpTrace);
		return persistent;
	}

	public boolean isFinished() {
		return endTime != 0;
	}

	void generateNextSliceTime() {
		nextSliceTime = startTime + initialPeriodMs * (1 << sliceTimeCount++);
	}
}