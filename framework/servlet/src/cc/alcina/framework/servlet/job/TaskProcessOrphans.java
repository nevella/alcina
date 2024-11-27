package cc.alcina.framework.servlet.job;

import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskProcessOrphans extends PerformerTask.Remote {
	@Override
	public void run() throws Exception {
		JobRegistry.get().processOrphans();
	}
}
