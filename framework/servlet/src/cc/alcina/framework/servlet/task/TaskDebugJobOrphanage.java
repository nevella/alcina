package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskDebugJobOrphanage extends PerformerTask {
	public long jobId;

	@Override
	public void run() throws Exception {
		JobContext.info(JobRegistry.get().debugOrphanage(jobId));
	}
}
