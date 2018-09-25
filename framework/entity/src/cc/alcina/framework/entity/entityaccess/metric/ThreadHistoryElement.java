package cc.alcina.framework.entity.entityaccess.metric;

import java.util.Date;

import cc.alcina.framework.entity.entityaccess.cache.DomainCacheLockState;

public class ThreadHistoryElement {
	public Date date;

	public ThreadInfoSer threadInfo;

	public long domainCacheLockTime;

	public DomainCacheLockState lockState;

	public long domainCacheWaitTime;
}
