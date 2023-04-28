package cc.alcina.framework.servlet.schedule;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Task;

public abstract class KnownTask extends PerformerTask
		implements KnownJobPerformer {
	@Override
	public void performAction(Task task) throws Exception {
		performActionExKnownContext(task);
	}

	@Override
	public void performActionInKnownContext(Task task) throws Exception {
		Preconditions.checkArgument(task == this);
		run();
	}
}
