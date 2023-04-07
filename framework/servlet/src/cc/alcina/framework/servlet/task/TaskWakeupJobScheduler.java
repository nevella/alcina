package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.job.JobRegistry;

public class TaskWakeupJobScheduler extends ServerTask {
	@Override
	public void run() throws Exception  {
		JobRegistry.get().wakeupScheduler();
	}
}
