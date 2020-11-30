package cc.alcina.framework.entity.persistence.metric;

import java.util.Date;
import java.util.stream.Collectors;

import cc.alcina.framework.entity.persistence.cache.DomainStoreLockState;
import cc.alcina.framework.entity.persistence.cache.DomainStoreWaitStats;

public class ThreadHistoryElement {
	public Date date;

	public ThreadInfoSer threadInfo;

	public long domainCacheLockTime;

	public DomainStoreLockState lockState;

	public DomainStoreWaitStats waitStats;

	public long domainCacheWaitTime;

	public int elidedStacktraceFrameCount;

	public void elideIfMoreLinesThan(int maxFrames) {
		int size = threadInfo.stackTrace.size();
		if (size > maxFrames) {
			if (size > 10000) {
				int debug = 3;
			}
			threadInfo.stackTrace = threadInfo.stackTrace.stream()
					.limit(maxFrames).collect(Collectors.toList());
			this.elidedStacktraceFrameCount += size - maxFrames;
		}
	}
}
