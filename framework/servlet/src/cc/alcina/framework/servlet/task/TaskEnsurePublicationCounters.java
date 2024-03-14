package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskEnsurePublicationCounters extends PerformerTask {
	@Override
	public void run() throws Exception {
		CommonPersistenceProvider.get().getCommonPersistence()
				.ensurePublicationCounters();
	}
}
