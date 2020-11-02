package cc.alcina.framework.servlet.schedule;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.servlet.job2.JobRegistry;
import cc.alcina.framework.servlet.schedule.StandardSchedules.RecurrentJobsExecutorSchedule;

public interface RecurrentJobQueueScheduler {
	default void schedule(Task task) {
		JobRegistry.get().schedule(task, new RecurrentJobsExecutorSchedule());
	}
	default void waitForScheduledTasks(){
		JobRegistry.get().waitForQueue(new RecurrentJobsExecutorSchedule().getQueueName());
	}
}
