package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskRunJob extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		long jobId = Long.parseLong(value);
		Job job = Job.byId(jobId);
		if (job == null) {
			logger.info("Job {} does not exist", jobId);
		} else if (job.provideIsComplete()) {
			logger.info("Job {} already completed", jobId);
		} else {
			job.setRunAt(null);
			job.setPerformer(ClientInstance.self());
			job.setState(JobState.PENDING);
			logger.info("TaskRunJob - future-to-pending - {}", job);
			Transaction.commit();
		}
	}
}
