package cc.alcina.framework.servlet.job2;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
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
	Map<String, Class<? extends Task>> queueNameSchedulableTasks = new LinkedHashMap<>();

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
	}

	private void ensureScheduled(Class<? extends Task> clazz,
			LocalDateTime nextRun) {
		logger.info("Ensure schedule - {} - {}", clazz, nextRun);
		Schedule schedule = getSchedule(clazz, false);
		JobRegistry.get().withJobMetadataLock(schedule.getQueueName(),
				schedule.isClustered(), () -> {
					Optional<? extends Job> pending = DomainDescriptorJob.get()
							.getUnallocatedJobsForQueue(schedule.getQueueName())
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

	private void scheduleJobs() {
		if (applicationStartup) {
			ensureQueues();
		}
		if (!jobRegistry.jobExecutors.isCurrentScheduledJobExecutor()) {
			return;
		}
		Transaction.ensureBegun();
		queueNameSchedulableTasks.values().forEach(clazz -> {
			Schedule schedule = getSchedule(clazz, applicationStartup);
			LocalDateTime nextRun = schedule.getNext();
			if (nextRun != null) {
				ensureScheduled(clazz, nextRun);
			}
		});
		reassignOrphans();
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
					queueNameSchedulableTasks.put(schedule.getQueueName(),
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
					.getUnallocatedJobsForQueue(queueName).findFirst();
			if (pending.isPresent() && (pending.get().getRunAt() == null
					|| pending.get().getRunAt().before(new Date()))) {
				Job job = pending.get();
				Schedule schedule = getSchedule(job.getTask().getClass(),
						false);
				JobQueue queue = jobRegistry.ensureQueue(schedule);
				if (!queue.allocating) {
					// FIXME - move to soemwhere cleaner
					queue.allocating = true;
					AlcinaChildRunnable.runInTransactionNewThread(queueName,
							queue::loopAllocations);
				}
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
		return queueNameSchedulableTasks.values().stream().map(clazz -> {
			Schedule schedule = getSchedule(clazz, false);
			return DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(schedule.getQueueName())
					.findFirst().orElse(null);
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

		private String queueName;

		private RetryPolicy retryPolicy = new NoRetryPolicy();

		private boolean clustered = true;

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
