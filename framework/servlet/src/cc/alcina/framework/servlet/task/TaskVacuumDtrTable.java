package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskVacuumDtrTable extends PerformerTask {
	@Override
	public void run() throws Exception  {
		if (!Configuration.is("enabled")) {
			logger.warn("Disabled");
			return;
		}
		DomainStore.writableStore().getTransformSequencer().vacuumTables();
	}
}
