package cc.alcina.framework.servlet.job;

import java.util.Objects;

import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.servlet.servlet.JobServlet;

public interface HasRoutingPerformer<T extends Task> extends TaskPerformer<T> {
	RoutingPerformer routingPerformer();

	public static abstract class RoutingPerformer implements TaskPerformer {
		@Override
		public void performAction(Task task) throws Exception {
			String performerHostName = performerHostName();
			String taskUrl = JobServlet.createTaskUrl(performerHostName, task);
			JobContext.info("Performing {} on host {}",
					task.getClass().getSimpleName(), performerHostName);
			long performedId = JobServlet.invokeAsSystemUser(taskUrl);
			Job performedJob = Job.byId(performedId);
			JobContext.info("Performed {} on host {} as {}",
					task.getClass().getSimpleName(), performerHostName,
					performedJob);
			Job job = JobContext.get().getJob();
			job.setResultType(performedJob.getResultType());
			job.setResultMessage(performedJob.getResultMessage());
		}

		public abstract String performerHostName();

		public TaskPerformer route(TaskPerformer performer) {
			String hostName = performerHostName();
			if (Objects.equals(hostName, EntityLayerUtils.getLocalHostName())) {
				return performer;
			} else {
				return this;
			}
		}
	}
}
