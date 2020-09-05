package cc.alcina.framework.entity.entityaccess.metric;

import java.util.Date;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLockState;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreWaitStats;

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
		if(size>maxFrames){
			if(size>10000){
				int debug=3;
			}
			threadInfo.stackTrace = threadInfo.stackTrace.subList(0, maxFrames);
			this.elidedStacktraceFrameCount += size-maxFrames;
		}
	}
}
