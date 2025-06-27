package cc.alcina.framework.entity.persistence.metric;

import java.lang.management.ThreadInfo;
import java.util.Date;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLockState;
import cc.alcina.framework.entity.persistence.domain.DomainStoreWaitStats;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.InternalMetricType;

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
		this.startTime = startTime;
		this.thread = thread;
		this.type = type;
		this.metricName = metricName;
		threadHistory = new ThreadHistory();
	}

	void addSlice(ThreadInfo info, StackTraceElement[] stackTrace,
			long activeDomainStoreLockTime, long domainStoreWaitTime,
			DomainStoreLockState domainStoreLockState,
			DomainStoreWaitStats waitStats) {
		int maxStackLines = type.maxStackLines();
		int maxFrames = type.maxFrames();
		threadHistory.addElement(info, stackTrace, activeDomainStoreLockTime,
				domainStoreWaitTime, domainStoreLockState, maxStackLines,
				maxFrames, waitStats);
	}

	public InternalMetric asMetric() {
		if (persistent == null) {
			try {
				this.callContext = callContextProvider.get();
			} catch (Exception e) {
				this.callContext = SEUtilities.getFullExceptionMessage(e);
			}
			persistent = PersistentImpl
					.getNewImplementationInstance(InternalMetric.class);
			persistent.setCallName(metricName);
			persistent.setStartTime(new Date(startTime));
			persistent.setHostName(EntityLayerUtils.getLocalHostName());
			persistent.setObfuscatedArgs(callContext);
			persistent.setThreadName(
					Ax.format("%s:%s", thread.getName(), thread.getId()));
		}
		String lockType = Ax.blankToEmpty(persistent.getLockType());
		persistent.setEndTime(endTime == 0 ? null : new Date(endTime));
		persistent.setLockType(lockType);
		persistent.setSliceCount(threadHistory.getElementCount());
		persistent.setThreadHistory(threadHistory);
		persistent.setUpdateTime(new Date(lastSliceTime));
		persistent.setBlackboxData(InternalMetrics.get().getBlackboxData());
		persistent.setClientInstanceId(ClientInstance.current().getId());
		return persistent;
	}

	public boolean isFinished() {
		return endTime != 0;
	}

	public String logForBlackBox() {
		return Ax.format(
				"Thread: %s [%s] - Metric: %s - Start: %s - Persistent id: %s"
						+ "\nContext:\n%s",
				thread.getName(), thread.getId(), metricName,
				new Date(startTime), persistentId, callContext);
	}

	public void setPersistentId(long id) {
		persistentId = id;
		if (persistent != null) {
			persistent.setId(id);
		}
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

	public void updateContext(String context) {
		callContextProvider = () -> context;
		if (persistent != null) {
			persistent.setObfuscatedArgs(context);
		}
	}
}