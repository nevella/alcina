package cc.alcina.framework.servlet.job;

import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.lock.JobResource;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;


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
			task = TransientFieldTasks.get().perJobTask
					.remove(JobContext.get().getJob());
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
