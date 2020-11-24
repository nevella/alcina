package cc.alcina.framework.servlet.job;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.InnerAccess;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.Sx;

/**
 * <h2>TODO</h2>
 * <ul>
 * <li>Job reaping
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
	private LinkedList<ScheduleEvent> scheduleEvents = new LinkedList<>();

	Map<String, Schedule> schedulesByQueueName = new LinkedHashMap<>();

	JobScheduler(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		scheduleJobsQueueProcessor = new ScheduleJobsThread();
		scheduleJobsQueueProcessor.start();
		schedulesByQueueName = Registry.impls(Schedule.class).stream()
				.collect(AlcinaCollectors.toKeyMap(Schedule::getQueueName));
	}

	public void stopService() {
		timer.cancel();
		finished.set(true);
		synchronized (scheduleEvents) {
			scheduleEvents.notify();
		}
	}

	private boolean canAllocateClustered(Job job) {
		Optional<ScheduleProvider> scheduleProvider = Registry
				.optional(ScheduleProvider.class, job.getTask().getClass());
		// FIXME - mvcc.jobs.2 - (validation) all clustered jobs must have a
		// scheduleprovider
		if (scheduleProvider.isPresent()) {
			Schedule schedule = scheduleProvider.get()
					.getSchedule(job.getTask());
			if (schedule == null) {
				return false;
			}
			return jobRegistry.jobExecutors.isCurrentScheduledJobExecutor()
					|| schedule.isOnlyPerformOnLeader();
		} else {
			return false;
		}
	}

	private void ensureQueueNames() {
		List<Class> scheduleProviders = Registry.get()
				.allImplementations(ScheduleProvider.class).stream()
				.filter(ResourceUtilities::notDisabled).distinct()
				.collect(Collectors.toList());
		scheduleProviders.stream().map(Registry.get()::getLocations)
				.flatMap(Collection::stream).forEach(loc -> {
					Class<? extends Task> taskClass = loc.targetClass();
					Schedule schedule = getSchedule(taskClass, true);
					if (schedule != null) {
						queueNameSchedulableTasks.add(schedule.getQueueName(),
								taskClass);
					}
				});
	}

	private void ensureScheduled(Schedule schedule, Class<? extends Task> clazz,
			LocalDateTime nextRun) {
		logger.debug("Ensure schedule - {} - {}", clazz, nextRun);
		JobRegistry.get().withJobMetadataLock(schedule.getQueueName(),
				schedule.isClustered(), () -> {
					Optional<? extends Job> pending = DomainDescriptorJob.get()
							.getUnallocatedJobsForQueue(schedule.getQueueName(),
									false)
							.filter(job -> job.getRunAt() != null)
							.filter(job -> job.provideIsTaskClass(clazz))
							.filter(job -> job.getRunAt() != null).findFirst();
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
						Job job = jobRegistry.createJob(
								Reflections.newInstance(clazz), schedule,
								SEUtilities.toOldDate(nextRun), false);
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
				MethodContext.instance().withRootPermissions(true)
						.withWrappingTransaction().run(() -> {
							enqueueScheduleEvent(
									new ScheduleEvent(message, null));
						});
			}
		}, delay + 1);
	}

	private int getEventQueueLength() {
		synchronized (scheduleEvents) {
			return scheduleEvents.size();
		}
	}

	private void processOrphans() {
		// FIXME - mvcc.jobs - remove (with prej) all non-deserializable tasks
		List<ClientInstance> activeInstances = jobRegistry.jobExecutors
				.getActiveServers();
		logger.info("Process orphans - visible instances: {}", activeInstances);
		String visibleInstanceRegex = ResourceUtilities
				.get("visibleInstanceRegex");
		Date cutoff = SEUtilities
				.toOldDate(LocalDateTime.now().minusMinutes(1));
		Stream<? extends Job> allocatedIncomplete = DomainDescriptorJob.get()
				.getAllocatedIncompleteJobs()
				.filter(job -> job.provideCreationDateOrNow().before(cutoff))
				.filter(job -> job.getPerformer().toString()
						.matches(visibleInstanceRegex))
				.filter(job -> !job.provideParent().isPresent())
				.filter(job -> !activeInstances.contains(job.getPerformer()));
		Stream<? extends Job> pendingInactiveCreator = DomainDescriptorJob.get()
				.getPendingJobsWithInactiveCreator(activeInstances)
				.filter(job -> job.getCreator().toString()
						.matches(visibleInstanceRegex))
				.filter(job -> job.provideCreationDateOrNow().before(cutoff))
				.filter(job -> !job.provideParent().isPresent())
				.filter(job -> !activeInstances.contains(job.getCreator()));
		InnerAccess<Boolean> metadataLockHeld = new InnerAccess<>();
		metadataLockHeld.set(false);
		List<Job> toAbortOrReassign = Stream
				.concat(allocatedIncomplete, pendingInactiveCreator).distinct()
				.collect(Collectors.toList());
		MultikeyMap<Boolean> perQueueTaskClass = new UnsortedMultikeyMap<>(2);
		Runnable runnable = toAbortOrReassign.isEmpty() ? null
				: () -> toAbortOrReassign.forEach(job -> {
					logger.warn(
							"Aborting job {} (inactive client creator: {} - performer: {} - clustered: {})",
							job, job.getCreator(), job.getPerformer(),
							job.isClustered());
					if (ResourceUtilities.is("abortDisabled")) {
						logger.warn("Abort job");
						return;
					}
					job.setState(JobState.ABORTED);
					job.setEndTime(new Date());
					job.setResultType(JobResultType.DID_NOT_COMPLETE);
					// FIXME - mvcc.jobs - current logic is 'cancel children' -
					// may want to be finer-grained
					if (job.isClustered() && job.provideCanDeserializeTask()
							&& !job.provideParent().isPresent()) {
						/*
						 * Only retry max 1 combo of queue/taskclass
						 */
						Class<? extends Task> taskClass = job.getTask()
								.getClass();
						Schedule schedule = getSchedule(taskClass, false);
						if (schedule != null) {
							RetryPolicy retryPolicy = schedule.getRetryPolicy();
							if (retryPolicy.shouldRetry(job)) {
								Boolean retried = perQueueTaskClass.get(
										schedule.getQueueName(), taskClass);
								if (retried == null) {
									perQueueTaskClass.put(
											schedule.getQueueName(), taskClass,
											true);
									Job retry = jobRegistry.createJob(
											job.getTask(), schedule,
											job.getRunAt(), false);
									retry.setQueue(job.getQueue());
									job.createRelation(retry,
											JobRelationType.retry);
									logger.warn(
											"Rescheduling job  (retry) :: {} => {}",
											job, retry);
								}
							}
						}
					}
				});
		jobRegistry.withJobMetadataLock(null, false, runnable);
	}

	private synchronized void scheduleJobs(List<ScheduleEvent> events) {
		Stream<? extends Job> jobs = events.stream().map(e -> e.job)
				.filter(Objects::nonNull);
		Transaction.ensureBegun();
		if (applicationStartup) {
			ensureQueueNames();
			jobs = DomainDescriptorJob.get().getNotCompletedJobs();
		}
		/*
		 * orphans before scheduling (since we may resubmit timewise-constrained
		 * tasks)
		 */
		if (jobRegistry.jobExecutors.isCurrentScheduledJobExecutor()) {
			processOrphans();
		}
		queueNameSchedulableTasks.allValues().forEach(clazz -> {
			Schedule schedule = getSchedule(clazz, applicationStartup);
			if (schedule == null) {
				return;
			}
			/*
			 * During app startup, allow all vms to schedule clustered jobs
			 * (since code defining 'next' may have changed). That said, the
			 * scheduled job executor jvm will be the one doing the running...
			 */
			if (schedule.isClustered()
					&& !jobRegistry.jobExecutors.isCurrentScheduledJobExecutor()
					&& !applicationStartup) {
				return;
			}
			if (Sx.isTestServer() || Sx.isTest()) {
				if (ResourceUtilities.not(JobScheduler.class,
						"testSchedules")) {
					return;
				}
			}
			LocalDateTime nextRun = schedule.getNext();
			if (nextRun != null) {
				ensureScheduled(schedule, clazz, nextRun);
			}
		});
		Transaction.commit();
		ensureActiveQueues(jobs);
		applicationStartup = false;
	}

	void enqueueInitialScheduleEvent() {
		enqueueScheduleEvent(new ScheduleEvent("app-startup", null));
	}

	void enqueueLeaderChangedEvent() {
		enqueueScheduleEvent(new ScheduleEvent("leader-changed", null));
	}

	void enqueueScheduleEvent(ScheduleEvent event) {
		synchronized (scheduleEvents) {
			logger.debug("Enqueueing schedule event: {}", event);
			scheduleEvents.add(event);
			scheduleEvents.notifyAll();
		}
	}

	void ensureActiveQueues(Stream<? extends Job> jobs) {
		Stream<String> queueNames = Stream.concat(
				queueNameSchedulableTasks.keySet().stream(),
				jobs.filter(Job::isClustered)
						.filter(job -> job.getState() == JobState.PENDING)
						.filter(this::canAllocateClustered).map(Job::getQueue))
				.filter(Objects::nonNull).distinct();
		queueNames.forEach(queueName -> {
			Optional<? extends Job> pending = DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(queueName, true)
					.filter(Job::provideIsAllocatable).findFirst();
			if (pending.isPresent()
					&& pending.get().provideCanDeserializeTask()) {
				Job job = pending.get();
				Schedule schedule = getSchedule(job.getTask());
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

	Schedule getSchedule(Task task) {
		Optional<ScheduleProvider> scheduleProvider = Registry
				.optional(ScheduleProvider.class, task.getClass());
		if (!scheduleProvider.isPresent()) {
			return null;
		}
		Schedule schedule = scheduleProvider.get().getSchedule(task);
		return schedule;
	}

	List<Job> getScheduledJobs() {
		return queueNameSchedulableTasks.allValues().stream().map(clazz -> {
			Schedule schedule = getSchedule(clazz, false);
			return DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(schedule.getQueueName(), false)
					.filter(job -> job.provideIsTaskClass(clazz))
					.filter(job -> job.getRunAt() != null).findFirst()
					.orElse(null);
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	Schedule getScheduleForQueueEventAndOrJob(String name, Job job) {
		Schedule schedule = name == null ? null
				: schedulesByQueueName.get(name);
		boolean ignore = false;
		if (schedule == null) {
			if (EntityLayerObjects.get()
					.isForeignClientInstance(job.getCreator())) {
				if (!job.isClustered() || !ResourceUtilities
						.is(JobRegistry.class, "joinClusteredJobs")) {
					ignore = true;
				}
			}
		}
		if (!ignore) {
			Class<? extends Task> clazz = job.getTask().getClass();
			Optional<ScheduleProvider> scheduleProvider = Registry
					.optional(ScheduleProvider.class, clazz);
			if (scheduleProvider.isPresent()) {
				schedule = scheduleProvider.get().getSchedule(job);
				if (schedule != null) {
					logger.warn("Creating queue from provider {} for job {}",
							scheduleProvider.get().getClass().getSimpleName(),
							job);
				}
			}
		}
		return schedule;
	}

	void onScheduleEvent(ScheduleEvent event) {
		enqueueScheduleEvent(event);
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

	@RegistryLocation(registryPoint = RetentionPolicy.class)
	public static class RetentionPolicy {
		public boolean retain(Job job) {
			if (job.provideIsNotComplete()) {
				return true;
			}
			Date date = job.resolveCompletionDate();
			if (date == null) {
				// invalid job, clear
				return false;
			}
			LocalDateTime end = SEUtilities.toLocalDateTime(date);
			long days = ChronoUnit.DAYS.between(end, LocalDateTime.now());
			long minutes = ChronoUnit.MINUTES.between(end, LocalDateTime.now());
			if (job.resolveState() == JobState.ABORTED) {
				return minutes < 5;
			}
			if (job.resolveState() == JobState.CANCELLED) {
				return days == 0;
			}
			return days < 7;
		}
	}

	public interface RetryPolicy {
		static RetryPolicy retryNTimes(int nTimes) {
			return new RetryNTimesPolicy(nTimes);
		}

		boolean shouldRetry(Job failedJob);
	}

	@RegistryLocation(registryPoint = Schedule.class)
	public static class Schedule {
		private ExecutorServiceProvider exexcutorServiceProvider = new ExecutorServiceProvider() {
			@Override
			public ExecutorService getService() {
				return Executors.newSingleThreadExecutor(
						new NamedThreadFactory("custom-name") {
							@Override
							public Thread newThread(Runnable r) {
								Thread thread = super.newThread(r);
								thread.setName(queueName + "-executor");
								return thread;
							}
						});
			}
		};

		private int queueMaxConcurrentJobs = Integer.MAX_VALUE;

		private LocalDateTime next;

		private String queueName = getClass().getSimpleName()
				.replaceFirst("(.+)Schedule", "$1");

		private RetryPolicy retryPolicy = new NoRetryPolicy();

		private boolean clustered = true;

		private boolean timewiseLimited;

		private boolean runSameQueueScheduledAsSubsequent;

		private boolean onlyPerformOnLeader;

		public long calculateMaxAllocated(JobQueue queue, int allocated,
				int jobCountForQueue, int unallocatedJobCountForQueue) {
			return -1;
		}

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

		public boolean isOnlyPerformOnLeader() {
			return onlyPerformOnLeader;
		}

		public boolean isRunSameQueueScheduledAsSubsequent() {
			return this.runSameQueueScheduledAsSubsequent;
		}

		public boolean isTimewiseLimited() {
			return timewiseLimited;
		}

		@Override
		public String toString() {
			return Ax.format("Schedule %s::%s - queue: %s; clustered: %s",
					getClass().getSimpleName(), Integer.toHexString(hashCode()),
					queueName, clustered);
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

		public Schedule withOnlyPerformOnLeader(boolean onlyPerformOnLeader) {
			this.onlyPerformOnLeader = onlyPerformOnLeader;
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

		public Schedule withRunSameQueueScheduledAsSubsequent(
				boolean runSameQueueScheduledAsSubsequent) {
			this.runSameQueueScheduledAsSubsequent = runSameQueueScheduledAsSubsequent;
			return this;
		}

		public Schedule withSubJobQueueName(Job job) {
			withQueueName(Ax.format("%s::%s::sub", job.getTaskClassName(),
					job.toLocator().toString()));
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
			setName("Schedule-jobs-queue-" + hashCode());
			while (!finished.get()) {
				try {
					List<ScheduleEvent> events = null;
					synchronized (scheduleEvents) {
						if (getEventQueueLength() == 0) {
							scheduleEvents.wait();
							logger.debug("Woken from toFire queue wakeup");
						}
						if (getEventQueueLength() > 0) {
							events = scheduleEvents.stream()
									.collect(Collectors.toList());
							scheduleEvents.clear();
							logger.debug(
									"Removed {} events from toFire (and cleared): {}",
									scheduleEvents.size());
						}
					}
					if (events != null && !finished.get()) {
						try {
							Transaction.ensureBegun();
							ThreadedPermissionsManager.cast().pushSystemUser();
							DomainStore.waitUntilCurrentRequestsProcessed();
							scheduleJobs(events);
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
	}

	public interface ScheduleProvider<T extends Task> {
		default boolean checkForUnallocatedOrIncomplete(String queueName) {
			if (DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(queueName, false).count() > 0) {
				JobContext.info("{} - already scheduled", queueName);
				return false;
			}
			if (DomainDescriptorJob.get()
					.getIncompleteJobCountForActiveQueue(queueName) > 0) {
				JobContext.info("{} - already running", queueName);
				return false;
			}
			return true;
		}

		default Schedule getSchedule(Class<? extends T> taskClass,
				boolean onAppplicationStart) {
			return null;
		}

		default Schedule getSchedule(Job job) {
			/*
			 * For distributed subjob schedule creation
			 */
			return null;
		}

		default Schedule getSchedule(T task) {
			return getSchedule((Class<T>) task.getClass(), false);
		}
	}

	private static class RetryNTimesPolicy implements RetryPolicy {
		private int nTimes;

		public RetryNTimesPolicy(int nTimes) {
			this.nTimes = nTimes;
		}

		@Override
		public boolean shouldRetry(Job failedJob) {
			int counter = 0;
			Job cursor = failedJob;
			while (true) {
				counter++;
				Optional<Job> precedingJob = cursor.getToRelations().stream()
						.filter(rel -> rel.getType() == JobRelationType.retry)
						.findFirst().map(JobRelation::getFrom);
				if (precedingJob.isPresent()) {
					cursor = precedingJob.get();
				} else {
					break;
				}
			}
			return counter <= nTimes;
		}
	}

	static class CurrentThreadExecutorServiceProvider
			implements ExecutorServiceProvider {
		@Override
		public ExecutorService getService() {
			return new CurrentThreadExecutorService();
		}
	}

	static class ScheduleEvent {
		String queueName;

		Job job;

		public ScheduleEvent() {
		}

		public ScheduleEvent(String queueName, Job job) {
			this.queueName = queueName;
			this.job = job;
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}
	}
}
