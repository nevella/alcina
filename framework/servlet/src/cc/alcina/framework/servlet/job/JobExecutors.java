package cc.alcina.framework.servlet.job;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

// FIXME - mvcc.jobs.1a - all api in use?
public interface JobExecutors {
	void addScheduledJobExecutorChangeConsumer(
			Consumer<Boolean> changeConsumer);

	Object allocationLock(String path, boolean acquire);

	List<ClientInstance> getActiveServers();

	default Timestamp getJobMetadataLockTimestamp(String path) {
		return null;
	}

	default int getMaxConsistencyJobCount() {
		return 0;
	}

	default boolean isCurrentOrphanage() {
		return isCurrentScheduledJobExecutor();
	}

	boolean isCurrentScheduledJobExecutor();

	boolean isHighestBuildNumberInCluster();
}