package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;

public class TaskDumpProperties extends ServerTask {
	@Override
	public void run() throws Exception  {
		logger.info(AppLifecycleServletBase.get().dumpCustomProperties());
	}
}
