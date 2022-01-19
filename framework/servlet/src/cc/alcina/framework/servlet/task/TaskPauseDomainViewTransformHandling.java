package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;
import cc.alcina.framework.servlet.domain.view.DomainViews;

/*
 * Assists targetted debugging of view node generation
 */
public class TaskPauseDomainViewTransformHandling extends AbstractTaskPerformer {
	
	private boolean pause;
	public boolean isPause() {
		return this.pause;
	}
	public void setPause(boolean pause) {
		this.pause = pause;
	}
	@Override
	protected void run0() throws Exception {
		DomainViews.get().pauseDomainViewTransforms(pause);
	}
}
