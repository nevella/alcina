package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskWakeupJobScheduler extends PerformerTask {
	@Override
	public void run() throws Exception {
		JobRegistry.get().wakeupScheduler();
	}
}
