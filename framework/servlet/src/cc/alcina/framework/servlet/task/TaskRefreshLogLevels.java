package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskRefreshLogLevels extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}
}
