package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.AlcinaParallel.AlcinaParallelJobChecker;

@Registration(
	value = AlcinaParallelJobChecker.class,
	priority = Registration.Priority.PREFERRED_LIBRARY)
public class AlcinaParallelJobCheckerJobRegistry
		extends AlcinaParallelJobChecker {
	private JobContext jobContext;

	public AlcinaParallelJobCheckerJobRegistry() {
		jobContext = JobContext.get();
	}

	@Override
	public boolean isCancelled() {
		Transaction.ensureBegun();
		return jobContext != null && jobContext.getJob().provideIsComplete();
	}
}
