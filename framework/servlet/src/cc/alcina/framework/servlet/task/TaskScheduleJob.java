package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.job.JobRegistry.Builder;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskScheduleJob extends PerformerTask.Remote {
	public JobDomain.DefaultConsistencyPriorities priority;

	public String taskSerialized;

	@Override
	public void run() throws Exception {
		Builder builder = JobRegistry.createBuilder();
		builder.withTask(ReflectiveSerializer.deserialize(taskSerialized));
		if (priority != null) {
			builder.withInitialState(JobState.FUTURE_CONSISTENCY)
					.withConsistencyPriority(priority);
		}
		builder.create();
		Transaction.commit();
	}
}
