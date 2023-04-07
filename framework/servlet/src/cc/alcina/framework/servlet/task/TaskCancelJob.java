package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskCancelJob extends ServerTask {
	private long jobId;

	public long getJobId() {
		return this.jobId;
	}

	@Override
	public void run() throws Exception {
		Job job = Job.byId(jobId);
		if (job == null) {
			logger.info("Job {} does not exist", jobId);
		} else if (job.provideIsComplete()) {
			logger.info("Job {} already completed", jobId);
		} else {
			job.cancel();
			Transaction.commit();
			logger.info("Job {} cancelled", jobId);
		}
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public TaskCancelJob withJobId(long jobId) {
		this.jobId = jobId;
		return this;
	}
}
