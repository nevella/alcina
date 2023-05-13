package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/*
 * FIXME - jobs - probably most of these belong in TransactionEnvironment
 */
public interface JobEnvironment {
	ClientInstance getPerformerInstance();

	Transaction getScheduleEventTransaction();

	default boolean isInTransactionMultipleTxEnvironment() {
		return TransactionEnvironment.get().isMultiple()
				&& TransactionEnvironment.get().isInTransaction();
	}

	boolean isPersistent();

	boolean isTrackMetrics();

	void onJobCreated(Job job);

	void prepareUserContext(Job job);

	void processScheduleEvent(Runnable runnable);

	void runInTransaction(ThrowingRunnable runnable);

	void runInTransactionThread(Runnable runnable);

	void setAllocatorThreadName(String name);

	void waitUntilCurrentRequestsProcessed();
}
