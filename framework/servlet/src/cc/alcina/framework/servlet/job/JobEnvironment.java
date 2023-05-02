package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

public interface JobEnvironment {
	boolean canCreateFutures();

	void commit();

	ClientInstance getPerformerInstance();

	Transaction getScheduleEventTransaction();

	boolean isPersistent();

	void onJobCreated(Job job);

	void processScheduleEvent(Runnable runnable);

	void runInTransaction(ThrowingRunnable runnable);

	void waitUntilCurrentRequestsProcessed();
}
