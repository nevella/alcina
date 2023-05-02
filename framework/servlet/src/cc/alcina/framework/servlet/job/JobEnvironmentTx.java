package cc.alcina.framework.servlet.job;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.MethodContext;

/**
 * The default environment, backed by the mvcc writable DomainStore
 * 
 * @author nick@alcina.cc
 *
 */
class JobEnvironmentTx implements JobEnvironment {
	@Override
	public boolean canCreateFutures() {
		return true;
	}

	@Override
	public void commit() {
		Transaction.commit();
	}

	@Override
	public ClientInstance getPerformerInstance() {
		return EntityLayerObjects.get().getServerAsClientInstance();
	}

	@Override
	public Transaction getScheduleEventTransaction() {
		Transaction current = Transaction.current();
		Preconditions.checkNotNull(current);
		return current;
	}

	@Override
	public boolean isPersistent() {
		return true;
	}

	@Override
	public void onJobCreated(Job job) {
		// NOOP
	}

	@Override
	public void processScheduleEvent(Runnable runnable) {
		MethodContext.instance().withWrappingTransaction()
				.withRootPermissions(true)
				.run(ThrowingRunnable.wrapRunnable(runnable));
	}

	@Override
	public void runInTransaction(ThrowingRunnable runnable) {
		MethodContext.instance().withWrappingTransaction().run(runnable);
	}

	@Override
	public void waitUntilCurrentRequestsProcessed() {
		DomainStore.waitUntilCurrentRequestsProcessed();
	}
}
