package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;

public class TaskDumpProperties extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		logger.info(AppLifecycleServletBase.get().dumpCustomProperties());
	}
}
