package cc.alcina.framework.servlet.schedule;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.knowns.KnownJob;

public interface KnownJobPerformer<T extends Task> {
	KnownJob getKnownJob();

	default void performActionExKnownContext(T task) throws Exception {
		KnownJob knownJob = getKnownJob();
		try {
			if (knownJob != null) {
				knownJob.startJob();
			}
			performActionInKnownContext(task);
			if (knownJob != null && JobContext.has()) {
				getKnownJob()
						.jobOk(JobContext.get().getJob().getResultMessage());
			}
		} catch (Exception e) {
			if (knownJob != null) {
				getKnownJob().jobError(e);
			}
			throw e;
		}
	}

	void performActionInKnownContext(T task) throws Exception;
}
