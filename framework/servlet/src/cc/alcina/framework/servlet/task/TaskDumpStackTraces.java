package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskDumpStackTraces extends PerformerTask.Remote {
	@Override
	public void run() throws Exception {
		SEUtilities.dumpAllThreads();
	}
}
