package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskEnsurePublicationCounters extends ServerTask {
	@Override
	public void run() throws Exception  {
		CommonPersistenceProvider.get().getCommonPersistence()
				.ensurePublicationCounters();
	}
}
