package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskVacuumDtrTable extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		if (!ResourceUtilities.is("enabled")) {
			logger.warn("Disabled");
			return;
		}
		DomainStore.writableStore().getTransformSequencer().vacuumTables();
	}
}
