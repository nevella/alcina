package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskEnsurePublicationCounters extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		CommonPersistenceProvider.get().getCommonPersistence()
				.ensurePublicationCounters();
		;
	}
}
