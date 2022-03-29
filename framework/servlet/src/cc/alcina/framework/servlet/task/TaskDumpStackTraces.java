package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskDumpStackTraces extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		SEUtilities.dumpAllThreads();
	}
}
