package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.domain.view.DomainViews;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskClearDomainViews extends ServerTask {
	@Override
	public void run() throws Exception {
		DomainViews.get().clearTrees();
	}
}