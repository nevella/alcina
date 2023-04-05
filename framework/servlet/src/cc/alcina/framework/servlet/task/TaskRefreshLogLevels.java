package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskRefreshLogLevels extends ServerTask {
	@Override
	public void run() throws Exception  {
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}
}
