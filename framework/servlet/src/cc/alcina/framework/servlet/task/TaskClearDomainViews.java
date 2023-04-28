package cc.alcina.framework.servlet.task;

import cc.alcina.framework.servlet.domain.view.DomainViews;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskClearDomainViews extends PerformerTask {
	@Override
	public void run() throws Exception {
		DomainViews.get().clearTrees();
	}
}