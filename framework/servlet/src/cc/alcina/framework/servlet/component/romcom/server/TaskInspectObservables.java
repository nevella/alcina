package cc.alcina.framework.servlet.component.romcom.server;

import cc.alcina.framework.servlet.component.traversal.TraversalHistories;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * Log romcom observable eviction status
 */
public class TaskInspectObservables extends PerformerTask.Remote {
	@Override
	public void run() throws Exception {
		TraversalHistories.get().observables.checkEvict(true);
	}
}
