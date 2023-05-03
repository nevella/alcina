package cc.alcina.framework.servlet.job;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.NonRootTask;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
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
	public void prepareUserContext(Job job) {
		Task task = job.getTask();
		if (task instanceof NonRootTask) {
			ThreadedPermissionsManager.cast().pushUser(
					((NonRootTask) task).provideIUser(job),
					LoginState.LOGGED_IN);
		} else {
			ThreadedPermissionsManager.cast().pushSystemUser();
		}
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
	public void runInTransactionThread(Runnable runnable) {
		runnable.run();
	}

	@Override
	public void setAllocatorThreadName(String name) {
		Thread.currentThread().setName(name);
	}

	@Override
	public void waitUntilCurrentRequestsProcessed() {
		DomainStore.waitUntilCurrentRequestsProcessed();
	}
}
