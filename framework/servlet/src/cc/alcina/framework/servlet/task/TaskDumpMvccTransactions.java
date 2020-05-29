package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transactions;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskDumpMvccTransactions extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		actionLogger.warn(Transactions.stats().describeTransactions());
	}
}
