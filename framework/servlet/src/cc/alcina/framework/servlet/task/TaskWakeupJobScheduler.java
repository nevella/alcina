package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.job.JobRegistry;

public class TaskWakeupJobScheduler extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		JobRegistry.get().wakeupScheduler();
	}
}
