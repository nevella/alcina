package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskCancelJob extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		long jobId = Long.parseLong(value);
		Job job = Job.byId(jobId);
		if (job == null) {
			slf4jLogger.info("Job {} does not exist", jobId);
		} else if (job.provideIsComplete()) {
			slf4jLogger.info("Job {} already completed", jobId);
		} else {
			job.cancel();
			Transaction.commit();
			slf4jLogger.info("Job {} cancelled", jobId);
		}
	}
}
