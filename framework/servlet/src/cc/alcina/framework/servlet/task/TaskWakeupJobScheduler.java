package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.job.JobRegistry;

public class TaskWakeupJobScheduler extends PerformerTask {
	@Override
	public void run() throws Exception  {
		JobRegistry.get().wakeupScheduler();
	}
}
