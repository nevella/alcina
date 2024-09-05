package cc.alcina.framework.servlet.task.dev;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.process.observer.job.JobObserver;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * A developer task which emits job observables (as a demonstration of the
 * functionality). Note that the configuration property
 * JobScheduler.observeJobEvents must be true for events to be collected
 */
public class TaskEmitJobObservables extends PerformerTask
		implements Task.RemotePerformable {
	public TaskEmitJobObservables() {
	}

	@Override
	public void run() throws Exception {
		JobObserver.getHistory(JobContext.get().getJob().toLocator()).sequence()
				.withIncludeMvccObservables(true).exportLocal();
		logger.info("TaskEmitJobObservables - complete");
	}
}
