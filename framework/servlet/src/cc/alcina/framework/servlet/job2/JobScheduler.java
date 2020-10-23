package cc.alcina.framework.servlet.job2;

import java.time.LocalDateTime;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.job.Task.ExexcutorServiceProvider;
import cc.alcina.framework.common.client.job.Task.NoRetryPolicy;
import cc.alcina.framework.common.client.job.Task.RetryPolicy;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * <ul>
 * <li>
 * <li>
 * <ul>
 * 
 * @author nick@alcina.cc
 *
 */
public class JobScheduler {
	Schedule getSchedule(Task task) {
		return Registry.optional(ScheduleProvider.class, getClass())
				.map(p -> p.getSchedule(task, false)).orElse(null);
	}

	public static class Schedule {
		private ExexcutorServiceProvider exexcutorServiceProvider;

		private int queueMaxConcurrentJobs = Integer.MAX_VALUE;

		private int maxTaskPendingJobs = 1;

		private LocalDateTime next;

		private String queueName;

		private RetryPolicy retryPolicy = new NoRetryPolicy();

		private boolean clustered;

		public ExexcutorServiceProvider getExcutorServiceProvider() {
			return exexcutorServiceProvider;
		}

		public int getMaxTaskPendingJobs() {
			return this.maxTaskPendingJobs;
		}

		public LocalDateTime getNext() {
			return next;
		}

		public int getQueueMaxConcurrentJobs() {
			return queueMaxConcurrentJobs;
		}

		public String getQueueName() {
			return queueName;
		}

		public RetryPolicy getRetryPolicy() {
			return retryPolicy;
		}

		public boolean isClustered() {
			return clustered;
		}

		public Schedule withClustered(boolean clustered) {
			this.clustered = clustered;
			return this;
		}

		public Schedule withExexcutorServiceProvider(
				ExexcutorServiceProvider exexcutorServiceProvider) {
			this.exexcutorServiceProvider = exexcutorServiceProvider;
			return this;
		}

		public Schedule withMaxTaskPendingJobs(int maxTaskPendingJobs) {
			this.maxTaskPendingJobs = maxTaskPendingJobs;
			return this;
		}

		public Schedule withNext(LocalDateTime next) {
			this.next = next;
			return this;
		}

		public Schedule withQueueMaxConcurrentJobs(int queueMaxConcurrentJobs) {
			this.queueMaxConcurrentJobs = queueMaxConcurrentJobs;
			return this;
		}

		public Schedule withQueueName(String queueName) {
			this.queueName = queueName;
			return this;
		}

		public Schedule withRetryPolicy(RetryPolicy retryPolicy) {
			this.retryPolicy = retryPolicy;
			return this;
		}
	}

	public interface ScheduleProvider {
		public Schedule getSchedule(Task task, boolean onAppplicationStart);
	}
}
