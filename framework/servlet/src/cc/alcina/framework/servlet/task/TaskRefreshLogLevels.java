package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskRefreshLogLevels extends PerformerTask {
	@Override
	public void run() throws Exception  {
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}
}
