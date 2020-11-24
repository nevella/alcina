package cc.alcina.framework.servlet.schedule;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.schedule.StandardSchedules.RecurrentJobsExecutorSchedule;

/*
 * Make sure to call waitForScheduledTasks after any call(s) to schedule()
 */
public interface RecurrentJobQueueScheduler {
	default Job schedule(Task task) {
		JobContext.get().setSchedulingSubTasks(true);
		return JobRegistry.get().schedule(task,
				new RecurrentJobsExecutorSchedule());
	}

	default void waitForScheduledTasks() {
		// will flush scheduled tasks
		JobContext.get().setSchedulingSubTasks(false);
		// other than this, NOOP - the calling job will not end until all child
		// jobs complete
	}
}
