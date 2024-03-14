package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;

public class TaskDumpProperties extends PerformerTask {
	@Override
	public void run() throws Exception {
		logger.info(AppLifecycleServletBase.get().dumpCustomProperties());
	}
}
