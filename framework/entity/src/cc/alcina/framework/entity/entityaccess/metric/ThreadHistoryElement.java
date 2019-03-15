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

    public void elideIfMoreLinesThan(int max) {
        elidedStacktraceFrameCount = CommonUtils
                .elideList(threadInfo.stackTrace, max);
    }
}
