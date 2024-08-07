package cc.alcina.framework.servlet.job;

import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * Used to transfer transient (because possibly too large to serialize) tasks to
 * the receiving handler across threads
 */
@Registration.Singleton
public class TransientFieldTasks {
	public static TransientFieldTasks get() {
		return Registry.impl(TransientFieldTasks.class);
	}

	private ConcurrentHashMap<Job, Task> perJobTask = new ConcurrentHashMap<>();

	public void registerTask(Job job, Task task) {
		perJobTask.put(job, task);
	}

	public static class Resource implements JobResource {
		private Task task;

		@Override
		public void acquire() {
			Job job = JobContext.get().getJob();
			task = TransientFieldTasks.get().perJobTask.remove(job);
		}

		public <T extends Task> T getTask() {
			return (T) task;
		}

		@Override
		public void release() {
			// NOOP
		}
	}
}
