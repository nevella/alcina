package cc.alcina.framework.entity.entityaccess.metric;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreadInfoSer {
	public String threadName;

	public long threadId;

	public long blockedTime;

	public long blockedCount;

	public long waitedTime;

	public long waitedCount;

	public LockInfoSer lock;

	public String lockName;

	public long lockOwnerId;

	public String lockOwnerName;

	public boolean inNative;

	public boolean suspended;

	public Thread.State threadState;

	public List<StackTraceElement> stackTrace = new ArrayList<>();

	public List<MonitorInfoSer> lockedMonitors = new ArrayList<>();

	public List<LockInfoSer> lockedSynchronizers = new ArrayList<>();

	public ThreadInfoSer() {
	}

	public ThreadInfoSer(ThreadInfo info) {
		this.blockedCount = info.getBlockedCount();
		this.threadName = info.getThreadName();
		this.threadId = info.getThreadId();
		this.blockedTime = info.getBlockedTime();
		this.blockedCount = info.getBlockedCount();
		this.waitedTime = info.getWaitedTime();
		this.waitedCount = info.getWaitedCount();
		this.lock = info.getLockInfo() == null ? null
				: new LockInfoSer(info.getLockInfo());
		this.lockName = info.getLockName();
		this.lockOwnerId = info.getLockOwnerId();
		this.lockOwnerName = info.getLockOwnerName();
		this.inNative = info.isInNative();
		this.suspended = info.isSuspended();
		this.threadState = info.getThreadState();
		if (info.getStackTrace() != null) {
			stackTrace.addAll(Arrays.asList(info.getStackTrace()));
		}
		if (info.getLockedMonitors() != null) {
			Arrays.stream(info.getLockedMonitors()).map(MonitorInfoSer::new)
					.forEach(lockedMonitors::add);
		}
		if (info.getLockedSynchronizers() != null) {
			Arrays.stream(info.getLockedSynchronizers()).map(LockInfoSer::new)
					.forEach(lockedSynchronizers::add);
		}
	}
}
