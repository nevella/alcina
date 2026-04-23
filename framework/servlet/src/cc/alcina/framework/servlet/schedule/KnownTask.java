package cc.alcina.framework.servlet.schedule;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

public abstract class KnownTask extends PerformerTask
		implements KnownJobPerformer {
	@Override
	public void performAction(Task task) throws Exception {
		performActionExKnownContext(task);
	}

	@Bean(PropertySource.FIELDS)
	public abstract static class Remote extends KnownTask
			implements Task.RemotePerformable {
	}

	@Override
	public void performActionInKnownContext(Task task) throws Exception {
		Preconditions.checkArgument(task == this);
		run();
	}
}
