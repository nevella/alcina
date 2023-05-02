package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.local.LocalDomainQueue;
import cc.alcina.framework.servlet.local.LocalDomainStore;

/**
 * A non-mvcc environment
 * 
 * @author nick@alcina.cc
 *
 */
public class JobEnvironmentNonTx implements JobEnvironment {
	private LocalDomainStore localDomainStore;

	public JobEnvironmentNonTx(LocalDomainStore localDomainStore) {
		this.localDomainStore = localDomainStore;
	}

	@Override
	public boolean canCreateFutures() {
		return false;
	}

	@Override
	public void commit() {
		localDomainStore.commit();
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
		commit();
	}

	@Override
	public void processScheduleEvent(Runnable runnable) {
		runInTransaction(ThrowingRunnable.wrapRunnable(runnable));
	}

	@Override
	public void runInTransaction(ThrowingRunnable runnable) {
		LocalDomainQueue.run(runnable);
	}

	@Override
	public void waitUntilCurrentRequestsProcessed() {
		// NOOP - should be handled by single-threaded store event dispatch
	}

	class JobDomainLocal {
	}
}
