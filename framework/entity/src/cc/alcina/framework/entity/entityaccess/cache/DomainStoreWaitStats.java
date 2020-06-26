package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;

public class DomainStoreWaitStats {
	public List<DomainStoreWaitOnLockStat> waitingOnLockStats = new ArrayList<>();

	public static class DomainStoreWaitOnLockStat {
		public long threadId;

		public String threadName;

		public long persistedMetricId;

		public long lockTimeMs;

		public Long bestId() {
			return persistedMetricId != 0 ? persistedMetricId : threadId;
		}
	}
}