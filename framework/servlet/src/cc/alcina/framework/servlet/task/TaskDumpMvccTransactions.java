package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskDumpMvccTransactions extends ServerTask {
	@Override
	public void run() throws Exception  {
		logger.warn(Transactions.stats().describeTransactions());
	}
}
