package cc.alcina.framework.servlet.job;

import java.util.List;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
// FIXME - mvcc.jobs.1a - all api in use?
public interface JobExecutors {
	void addScheduledJobExecutorChangeConsumer(
			Consumer<Boolean> changeConsumer);

	void allocationLock(Job job, boolean acquire);

	List<ClientInstance> getActiveServers();

	boolean isCurrentScheduledJobExecutor();

	boolean isHighestBuildNumberInCluster();
}