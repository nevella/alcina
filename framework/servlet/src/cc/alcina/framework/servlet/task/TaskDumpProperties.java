package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskDumpProperties extends PerformerTask {
	@Override
	public void run() throws Exception {
		logger.info(Configuration.properties.asString(true));
	}
}
