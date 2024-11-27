package cc.alcina.framework.servlet.job;

import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * Remote won't work - by design, the production servers refuse it
 */
public class TaskProcessOrphans extends PerformerTask.Remote {
	@Override
	public void run() throws Exception {
		JobRegistry.get().processOrphans();
	}
}
