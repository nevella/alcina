package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskRotateDtrIndex extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		DomainStore.writableStore().refreshIndicies();
	}
}
