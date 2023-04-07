package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskDumpStackTraces extends ServerTask {
	@Override
	public void run() throws Exception  {
		SEUtilities.dumpAllThreads();
	}
}
