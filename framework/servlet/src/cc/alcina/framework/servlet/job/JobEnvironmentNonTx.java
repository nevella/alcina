package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.local.LocalDomainQueue;

/**
 * A non-mvcc environment
 * 
 * @author nick@alcina.cc
 *
 */
public class JobEnvironmentNonTx implements JobEnvironment {
	public JobEnvironmentNonTx() {
	}

	@Override
	public boolean canCreateFutures() {
		return false;
	}

	@Override
	public ClientInstance getPerformerInstance() {
		return ClientInstance.self();
	}

	@Override
	public Transaction getScheduleEventTransaction() {
		return null;
	}

	@Override
	public boolean isPersistent() {
		return false;
	}

	/*
	 * Since the non-tx system doesn't know (in general) about transactions,
	 * commit here
	 */
	@Override
	public void onJobCreated(Job job) {
		TransactionEnvironment.get().commit();
	}

	@Override
	public void prepareUserContext(Job job) {
		PermissionsManager.get().pushCurrentUser();
	}

	@Override
	public void processScheduleEvent(Runnable runnable) {
		runInTransactionThread(runnable);
	}

	@Override
	public void runInTransaction(ThrowingRunnable runnable) {
		LocalDomainQueue.run(runnable);
	}

	@Override
	public void runInTransactionThread(Runnable runnable) {
		runInTransaction(ThrowingRunnable.wrapRunnable(runnable));
	}

	@Override
	public void setAllocatorThreadName(String name) {
		// NOOP
	}

	@Override
	public void waitUntilCurrentRequestsProcessed() {
		// NOOP - should be handled by single-threaded store event dispatch
	}

	class JobDomainLocal {
	}
}
