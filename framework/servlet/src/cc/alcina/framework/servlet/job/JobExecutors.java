package cc.alcina.framework.servlet.job;

import java.util.List;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public interface JobExecutors {
	public static Topic<Boolean> topicRescheduleJobs = Topic.local();

	void addScheduledJobExecutorChangeConsumer(
			Consumer<Boolean> changeConsumer);

	void allocationLock(String name, boolean clustered, boolean acquire);

	List<ClientInstance> getActiveServers();

	boolean isCurrentScheduledJobExecutor();
}