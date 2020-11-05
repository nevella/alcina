package cc.alcina.framework.servlet.job2;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.MethodContext;

/**
 * <h2>TODO</h2>
 * <ul>
 * <li>Job reaping
 * <li>Orphan adoption
 * <li>Cancel/stacktrace
 * <li>UI
 * <ul>
 * 
 * @author nick@alcina.cc
 *
 */
public class JobScheduler {
	Multimap<String, List<Class<? extends Task>>> queueNameSchedulableTasks = new Multimap<>();

	private JobRegistry jobRegistry;

	private Timer timer = new Timer();

	private LocalDateTime nextScheduled = null;

	Logger logger = LoggerFactory.getLogger(getClass());

	private boolean applicationStartup = true;

	private AtomicBoolean finished = new AtomicBoolean();

	private ScheduleJobsThread scheduleJobsQueueProcessor;

	/*
	 * all access locked on the field
	 */
	private LinkedList<String> toFire = new LinkedList<>();

	JobScheduler(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		scheduleJobsQueueProcessor = new ScheduleJobsThread();
		scheduleJobsQueueProcessor.start();
	}

	public void stopService() {
		timer.cancel();
		finished.set(true);
		synchronized (toFire) {
			toFire.notify();
		}
	}

	private void ensureScheduled(Schedule schedule, Class<? extends Task> clazz,
			LocalDateTime nextRun) {
		logger.info("Ensure schedule - {} - {}", clazz, nextRun);
		JobRegistry.get().withJobMetadataLock(schedule.getQueueName(),
				schedule.isClustered(), () -> {
					Optional<? extends Job> pending = DomainDescriptorJob.get()
							.getUnallocatedJobsForQueue(schedule.getQueueName(),
									false)
							.filter(job -> job.getTaskClassName()
									.equals(clazz.getName()))
							.findFirst();
					if (pending.isPresent()) {
						if (SEUtilities
								.toLocalDateTime(pending.get().getRunAt())
								.isAfter(nextRun)) {
							logger.info("Changed next run of {} to {}", pending,
									nextRun);
							pending.get()
									.setRunAt(SEUtilities.toOldDate(nextRun));
						}
					} else {
						Job job = jobRegistry
								.createJob(Reflections.newInstance(clazz));
						job.setQueue(schedule.getQueueName());
						job.setRunAt(SEUtilities.toOldDate(nextRun));
						job.setClustered(schedule.isClustered());
						logger.info("Scheduled job {} for {}", job, nextRun);
					}
				});
		if (nextScheduled == null || nextScheduled.isAfter(nextRun)) {
			nextScheduled = nextRun;
			ensureWakeup(clazz, nextRun);
		}
	}

	private synchronized void ensureWakeup(Class<? extends Task> clazz,
			LocalDateTime nextRun) {
		long delay = SEUtilities.toOldDate(nextRun).getTime()
				- System.currentTimeMillis();
		if (delay <= 0) {
			delay = 0;
		}
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String message = Ax.format("wakeup for %s - %s",
						clazz.getSimpleName(), nextRun);
				logger.info("Schedule {}", message);
				MethodContext.instance().withRootPermissions()
						.withWrappingTransaction().run(() -> {
							enqueueSchedule(message);
						});
			}
		}, delay + 1);
	}

	private int getToFireQueueLength() {
		synchronized (toFire) {
			return toFire.size();
		}
	}

	private void reassignOrphans() {
		// TODO Auto-generated method stub
	}

	private synchronized void scheduleJobs() {
		if (applicationStartup) {
			ensureQueues();
		}
		Transaction.ensureBegun();
		queueNameSchedulableTasks.allValues().forEach(clazz -> {
			Schedule schedule = getSchedule(clazz, applicationStartup);
			if (schedule == null) {
				return;
			}
			if (schedule.isClustered() && !jobRegistry.jobExecutors
					.isCurrentScheduledJobExecutor()) {
				return;
			}
			LocalDateTime nextRun = schedule.getNext();
			if (nextRun != null) {
				ensureScheduled(schedule, clazz, nextRun);
			}
		});
		if (jobRegistry.jobExecutors.isCurrentScheduledJobExecutor()) {
			reassignOrphans();
		}
		Transaction.commit();
		ensureActiveQueues();
		applicationStartup = false;
	}

	protected void ensureQueues() {
		List<Class> scheduleProviders = Registry.get()
				.allImplementations(ScheduleProvider.class).stream().distinct()
				.collect(Collectors.toList());
		scheduleProviders.stream().map(Registry.get()::getLocations)
				.flatMap(Collection::stream).forEach(loc -> {
					Class<? extends Task> taskClass = loc.targetClass();
					Schedule schedule = getSchedule(taskClass, true);
					queueNameSchedulableTasks.add(schedule.getQueueName(),
							taskClass);
				});
	}

	void enqueueInitialScheduleEvent() {
		enqueueSchedule("app-startup");
	}

	void enqueueSchedule(String id) {
		synchronized (toFire) {
			logger.debug("Enqueueing schedule event: {}", id);
			toFire.add(id);
			toFire.notifyAll();
		}
	}

	void ensureActiveQueues() {
		queueNameSchedulableTasks.keySet().forEach(queueName -> {
			Optional<? extends Job> pending = DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(queueName, true).findFirst();
			if (pending.isPresent()
					&& pending.get().provideIsNotRunAtFutureDate()) {
				Job job = pending.get();
				Schedule schedule = getSchedule(job.getTask().getClass(),
						false);
				JobQueue queue = jobRegistry.ensureQueue(schedule);
				queue.ensureAllocating();
				queue.onMetadataChanged();
			}
		});
	}

	Schedule getSchedule(Class<? extends Task> clazz,
			boolean applicationStartup) {
		Optional<ScheduleProvider> scheduleProvider = Registry
				.optional(ScheduleProvider.class, clazz);
		if (!scheduleProvider.isPresent()) {
			return null;
		}
		Schedule schedule = scheduleProvider.get().getSchedule(clazz,
				applicationStartup);
		return schedule;
	}

	List<Job> getScheduledJobs() {
		return queueNameSchedulableTasks.allValues().stream().map(clazz -> {
			Schedule schedule = getSchedule(clazz, false);
			return DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(schedule.getQueueName(), false)
					.filter(job -> job.getTaskClassName()
							.equals(clazz.getName()))
					.filter(job -> job.getRunAt() != null).findFirst()
					.orElse(null);
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	void onQueueChanged(String name) {
		enqueueSchedule(name);
	}

	public interface ExecutorServiceProvider {
		ExecutorService getService();
	}

	public static class NoRetryPolicy implements RetryPolicy {
		@Override
		public boolean shouldRetry(Job failedJob) {
			return false;
		}
	}

	public interface RetryPolicy {
		boolean shouldRetry(Job failedJob);
	}

	public interface Schedulable {
	}

	public static class Schedule {
		private ExecutorServiceProvider exexcutorServiceProvider = new CurrentThreadExecutorServiceProvider();

		private int queueMaxConcurrentJobs = Integer.MAX_VALUE;

		private LocalDateTime next;

		private String queueName = getClass().getSimpleName()
				.replaceFirst("(.+)Schedule", "$1");

		private RetryPolicy retryPolicy = new NoRetryPolicy();

		private boolean clustered = true;

		private boolean timewiseLimited;

		public ExecutorServiceProvider getExcutorServiceProvider() {
			return exexcutorServiceProvider;
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

		public boolean isTimewiseLimited() {
			return timewiseLimited;
		}

		public Schedule withClustered(boolean clustered) {
			this.clustered = clustered;
			return this;
		}

		public Schedule withExcutorServiceProvider(
				ExecutorServiceProvider exexcutorServiceProvider) {
			this.exexcutorServiceProvider = exexcutorServiceProvider;
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

		public Schedule withTimewiseLimited(boolean timewiseLimited) {
			this.timewiseLimited = timewiseLimited;
			return this;
		}
	}

	public class ScheduleJobsThread extends Thread {
		@Override
		public void run() {
			setName("Schedule-jobs-queue");
			while (!finished.get()) {
				try {
					String id = null;
					if (getToFireQueueLength() == 0) {
						synchronized (toFire) {
							toFire.wait();
							logger.debug("Woken from toFire queue wakeup");
						}
					}
					if (getToFireQueueLength() > 0) {
						synchronized (toFire) {
							id = toFire.removeFirst();
							logger.debug("Removed id from toFire: {}", id);
						}
					}
					if (id != null && !finished.get()) {
						try {
							Transaction.ensureBegun();
							ThreadedPermissionsManager.cast().pushSystemUser();
							DomainStore.waitUntilCurrentRequestsProcessed();
							publishScheduleEvent(id);
						} finally {
							Transaction.ensureBegun();
							ThreadedPermissionsManager.cast().popSystemUser();
							Transaction.ensureEnded();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void publishScheduleEvent(String id) {
			scheduleJobs();
		}
	}

	public interface ScheduleProvider {
		public Schedule getSchedule(Class<? extends Task> taskClass,
				boolean onAppplicationStart);
	}

	static class CurrentThreadExecutorServiceProvider
			implements ExecutorServiceProvider {
		@Override
		public ExecutorService getService() {
			return new CurrentThreadExecutorService();
		}
	}
}
