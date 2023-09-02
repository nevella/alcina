package cc.alcina.framework.servlet.job;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.Event;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.SubqueuePhase;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.MethodContext;

/**
 * <h2>TODO</h2>
 * <ul>
 * <li>Job reaping
 * <li>Cancel/stacktrace
 * <li>UI
 * <ul>
 *
 *
 */
public class JobScheduler {
	private JobRegistry jobRegistry;

	private Timer timer = new Timer();

	private LocalDateTime nextScheduledWakeup = null;

	Logger logger = LoggerFactory.getLogger(getClass());

	private boolean finished;

	private ScheduleJobsThread thread;

	private BlockingQueue<ScheduleEvent> events = new LinkedBlockingQueue<>();

	public Topic<Void> eventOcurred = Topic.create();

	private TopicListener<Event> queueEventListener = v -> enqueueEvent(
			new ScheduleEvent(v));

	private TopicListener<Void> futureConsistencyEventListener = v -> enqueueEvent(
			new ScheduleEvent(ScheduleEventType.FUTURE_CONSISTENCY_EVENT));

	Map<Job, JobAllocator> allocators = new ConcurrentHashMap<>();

	private ExecutorService allocatorService = Executors.newCachedThreadPool();

	TowardsAMoreDesirableSituation aMoreDesirableSituation;

	private JobEnvironment environment;

	JobScheduler(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		this.environment = jobRegistry.getEnvironment();
		JobDomain.get().queueEvents.add(queueEventListener);
		JobDomain.get().futureConsistencyEvents
				.add(futureConsistencyEventListener);
		JobDomain.get().fireInitialAllocatorQueueCreationEvents();
		thread = new ScheduleJobsThread();
		thread.start();
		if (environment.isPersistent()) {
			/*
			 * backup every-5-minute timer, FIXME - mvcc.jobs.1a - remove?
			 */
			LocalDateTime now = LocalDateTime.now();
			long untilNext5MinutesMillis = ChronoUnit.MILLIS.between(now,
					now.truncatedTo(ChronoUnit.MINUTES)
							.plusMinutes(5 - now.getMinute() % 5));
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					fireWakeup();
				}
			}, untilNext5MinutesMillis, 5 * TimeConstants.ONE_MINUTE_MS);
			aMoreDesirableSituation = new TowardsAMoreDesirableSituation(this);
			aMoreDesirableSituation.start();
		}
		environment.runInTransaction(() -> enqueueEvent(
				new ScheduleEvent(ScheduleEventType.APPLICATION_STARTUP)));
	}

	public JobAllocator awaitAllocator(Job job) {
		long timeout = System.currentTimeMillis() + TimeConstants.ONE_MINUTE_MS;
		while (System.currentTimeMillis() < timeout) {
			JobAllocator allocator = allocators.get(job);
			if (allocator != null) {
				return allocator;
			}
			synchronized (allocators) {
				try {
					allocators.wait(1000);
				} catch (InterruptedException e) {
					/*
					 * Only on app termination
					 */
					throw new RuntimeException(e);
				}
			}
		}
		/*
		 * An issue - fallback on resubmit. FIXME - mvcc.jobs.2 - add logging
		 * (is the event not received? out-of-order?)
		 */
		Transaction.ensureBegun();
		ResubmitPolicy policy = ResubmitPolicy.forJob(job);
		policy.visit(job);
		job.setState(JobState.ABORTED);
		job.setEndTime(new Date());
		job.setResultType(JobResultType.DID_NOT_COMPLETE);
		Transaction.commit();
		throw new RuntimeException("DEVEX::0 - awaitAllocator timeout");
	}

	public Predicate<Job> canModify(boolean scheduleClusterJobs,
			boolean scheduleVmLocalJobs) {
		Predicate<Job> canModify = job -> {
			if (Schedule.forTaskClass(job.provideTaskClass()).isVmLocal()) {
				return scheduleVmLocalJobs
						&& job.getCreator() == ClientInstance.self();
			} else {
				return scheduleClusterJobs;
			}
		};
		return canModify;
	}

	public Stream<Job> getToAbortOrReassign(
			List<ClientInstance> activeInstances, String visibleInstanceRegex,
			Date cutoff) {
		Date consistencyCutoff = SEUtilities
				.toOldDate(LocalDateTime.now().minusMinutes(120));
		return JobDomain.get().getIncompleteJobs()
				.filter(job -> job.provideCreationDateOrNow().before(cutoff))
				.filter(job -> job.getCreator().toString()
						.matches(visibleInstanceRegex))
				.filter(job -> job.getConsistencyPriority() == null
						|| (job.getStartTime() != null && job.getStartTime()
								.before(consistencyCutoff)))
				.filter(job -> (job.getPerformer() == null
						&& !activeInstances.contains(job.getCreator())
						&& /*
							 * don't abort if the creator has moved on but we
							 * have a still-active related processor (edge case)
							 */
						!job.provideRelatedSequential().stream()
								.anyMatch(relatedJob -> activeInstances
										.contains(relatedJob.getPerformer())))
						|| (job.getPerformer() != null && !activeInstances
								.contains(job.getPerformer())));
	}

	public void stopService() {
		events.add(new ScheduleEvent(ScheduleEventType.SHUTDOWN));
	}

	private void futuresToPending(ScheduleEvent event) {
		if (!SchedulingPermissions.canFutureToPending()) {
			return;
		}
		logger.info("futures to pending :: visible futures :: \n{}", JobDomain
				.get().getAllFutureJobs().collect(Collectors.toList()));
		JobDomain.get().getAllFutureJobs()
				.filter(job -> job.getRunAt().compareTo(new Date()) <= 0)
				.filter(SchedulingPermissions::canModifyFuture).forEach(job -> {
					Class<? extends Task> key = job.provideTaskClass();
					Schedule schedule = Schedule.forTaskClass(key);
					Optional<Job> earliestIncompleteScheduled = JobDomain.get()
							.getEarliestIncompleteScheduled(key,
									schedule.isVmLocal());
					if (schedule != null
							&& earliestIncompleteScheduled.isPresent()
							&& earliestIncompleteScheduled.get() != job
							&& schedule.isCancelIfExistingIncomplete()) {
						job.setState(JobState.ABORTED);
						logger.info(
								"Job scheduler - future-to-pending - ABORTED - {} - existingIncomplete - {}",
								job, earliestIncompleteScheduled.get());
					} else {
						job.setPerformer(ClientInstance.self());
						job.setState(JobState.PENDING);
						logger.info("Job scheduler - future-to-pending - {}",
								job);
					}
				});
	}

	private void processEvent(ScheduleEvent event) {
		environment.processScheduleEvent(() -> this.processEvent0(event));
	}

	private void processEvent0(ScheduleEvent event) {
		logger.trace("Received event {}", event);
		if (event.type == ScheduleEventType.WAKEUP) {
			if (nextScheduledWakeup != null && nextScheduledWakeup
					.compareTo(LocalDateTime.now()) <= 0) {
				nextScheduledWakeup = null;
			}
		}
		if (event.type.isRefreshFuturesEvent()) {
			/*
			 * orphans before scheduling (since we may resubmit
			 * timewise-constrained tasks)
			 */
			if (SchedulingPermissions.canProcessOrphans()) {
				try {
					processOrphans();
					Transaction.commit();
				} catch (Exception e) {
					logger.warn("DEVEX::0 - processOrphans", e);
					e.printStackTrace();
				}
			}
			jobRegistry.withJobMetadataLock(
					getClass().getName() + "::futuresToPending", () -> {
						futuresToPending(event);
						refreshFutures(event);
						Transaction.commit();
					});
		}
		if (event.type == ScheduleEventType.SHUTDOWN) {
			// FIXME - mvcc.jobs.1a - shutdown all fixed pools
			allocators.values()
					.forEach(allocator -> allocator.fireDeletedEvent());
			timer.cancel();
			allocatorService.shutdown();
			finished = true;
			return;
		}
		if (event.type == ScheduleEventType.ALLOCATION_EVENT) {
			AllocationQueue queue = event.queueEvent.queue;
			// FIXME - jobs - review exceptions. Were possibly due to (possibly
			// blocking) TowardsAMoreDesirableSituation call
			if (!TransactionEnvironment.get().isInActiveTransaction()) {
				Transaction.debugCurrentThreadTransaction();
				Transaction.ensureBegun();
			}
			boolean canAllocate = SchedulingPermissions.canAllocate(queue);
			if (canAllocate) {
				switch (event.queueEvent.type) {
				case CREATED: {
					JobAllocator allocator = new JobAllocator(queue,
							allocatorService);
					allocators.put(queue.job, allocator);
					allocator.enqueueEvent(event.queueEvent);
					synchronized (allocators) {
						allocators.notifyAll();
					}
				}
					break;
				case DELETED: {
					JobAllocator allocator = allocators.remove(queue.job);
					if (allocator != null) {
						allocator.enqueueEvent(event.queueEvent);
					}
				}
					break;
				default:
					// all other events are specific to the allocator, and
					// handled by the JobAllocator queue listener
					throw new UnsupportedOperationException();
				}
			}
		}
		eventOcurred.publish(null);
	}

	private void refreshFutures(ScheduleEvent event) {
		LocalDateTime wakeup = LocalDateTime.now().plusYears(1);
		List<Class<? extends Task>> taskClasses = (List) Registry.query()
				.setKeys(Schedule.class).childKeys()
				.collect(Collectors.toList());
		for (Class<? extends Task> key : taskClasses) {
			Schedule schedule = Schedule.forTaskClass(key);
			if (schedule == null) {
				continue;
			}
			LocalDateTime nextForTaskClass = schedule.getNext(
					event.type == ScheduleEventType.APPLICATION_STARTUP);
			if (nextForTaskClass == null) {
				continue;
			}
			Optional<Job> earliestIncompleteScheduled = JobDomain.get()
					.getEarliestIncompleteScheduled(key, schedule.isVmLocal());
			Optional<Job> earliestFuture = JobDomain.get()
					.getEarliestFuture(key, schedule.isVmLocal());
			if (earliestFuture.isPresent()) {
				Date nextDate = SEUtilities.toOldDate(nextForTaskClass);
				if (earliestFuture.get().getRunAt().after(nextDate)) {
					if (SchedulingPermissions
							.canModifyFuture(earliestFuture.get())) {
						logger.info("Changed next run of {} to {}",
								earliestFuture.get(), nextForTaskClass);
						earliestFuture.get().setRunAt(nextDate);
					}
				} else {
					if (SchedulingPermissions.canFutureToPending()) {
						nextForTaskClass = SEUtilities.toLocalDateTime(
								earliestFuture.get().getRunAt());
					}
				}
			} else {
				Job test = PersistentImpl
						.getNewImplementationInstance(Job.class);
				test.setTask(Reflections.newInstance(key));
				if (SchedulingPermissions.canCreateFuture(schedule)) {
					Job job = JobRegistry.createBuilder()
							.withTask(Reflections.newInstance(key))
							.withRunAt(nextForTaskClass).create();
					logger.info("Schedule new future job - {} to {}", job,
							nextForTaskClass);
				}
			}
			/*
			 * If there's an incomplete job of this task class, don't schedule a
			 * wakeup (otherwise we'll end up in a nasty perpetual-wakup loop)
			 */
			if (nextForTaskClass.isBefore(wakeup)
					&& !earliestIncompleteScheduled.isPresent()) {
				wakeup = nextForTaskClass;
			}
		}
		if (nextScheduledWakeup == null
				|| wakeup.isBefore(nextScheduledWakeup)) {
			if (wakeup.isBefore(LocalDateTime.now())) {
				logger.info("Firing wakeup - wakeup time {} is before now",
						wakeup);
				events.add(new ScheduleEvent(ScheduleEventType.WAKEUP));
			} else {
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						fireWakeup();
					}
				}, SEUtilities.toOldDate(wakeup));
				logger.debug("Scheduled wakeup for {}", wakeup);
			}
			nextScheduledWakeup = wakeup;
		}
	}

	ScheduleEvent enqueueEvent(ScheduleEvent event) {
		events.add(event);
		return event;
	}

	void enqueueLeaderChangedEvent() {
		MethodContext.instance().withWrappingTransaction()
				.call(() -> enqueueEvent(
						new ScheduleEvent(ScheduleEventType.LEADER_CHANGED)));
	}

	void fireWakeup() {
		MethodContext.instance().withWrappingTransaction()
				.run(() -> enqueueEvent(
						new ScheduleEvent(ScheduleEventType.WAKEUP)));
	}

	void processOrphans() {
		if (jobRegistry.jobExecutors.isHighestBuildNumberInCluster()) {
			JobDomain.get().getUndeserializableJobs().forEach(Job::delete);
		}
		List<ClientInstance> activeInstances = jobRegistry.jobExecutors
				.getActiveServers();
		logger.debug("Process orphans - visible instances: {}",
				activeInstances);
		/*
		 * handle flaky health/instances
		 */
		int minimumVisibleInstancesForOrphanProcessing = Configuration
				.getInt("minimumVisibleInstancesForOrphanProcessing");
		if (activeInstances
				.size() < minimumVisibleInstancesForOrphanProcessing) {
			logger.info(
					"Not processing orphans - visible instances size: {}, minimum size: {}",
					activeInstances.size(),
					minimumVisibleInstancesForOrphanProcessing);
			return;
		}
		String visibleInstanceRegex = Configuration.get("visibleInstanceRegex");
		Date cutoff = SEUtilities
				.toOldDate(LocalDateTime.now().minusMinutes(0));
		Date abortTime = new Date();
		long toOrphanCount = getToAbortOrReassign(activeInstances,
				visibleInstanceRegex, cutoff).count();
		if (toOrphanCount > 0) {
			jobRegistry.withJobMetadataLock(
					getClass().getName() + "::processOrphans", () -> {
						logger.info("Orphans remaining: {}", toOrphanCount);
						/*
						 * FIXME - mvcc.jobs.2 - performance of 'orphan' is
						 * fairly poor - db indicies? for the moment, let the
						 * gentle healing of time resolve this - and push a
						 * wakeup to the event queue (so we don't block on
						 * orphan) rather than looping here
						 */
						Stream<Job> doubleChecked = getToAbortOrReassign(
								activeInstances, visibleInstanceRegex, cutoff)
										.limit(200);
						doubleChecked.forEach(job -> {
							if (job.provideIsComplete()) {
								logger.warn(
										"Not aborting job {} - already complete",
										job);
								return;
							}
							logger.warn(
									"Aborting job {} (inactive client creator: {} - performer: {})",
									job, job.getCreator(), job.getPerformer());
							if (Configuration.is("abortDisabled")) {
								logger.warn(
										"(Would abort job - but abortDisabled)");
								return;
							}
							/* resubmit, then abort */
							ResubmitPolicy policy = ResubmitPolicy.forJob(job);
							policy.visit(job);
							job.setState(JobState.ABORTED);
							job.setEndTime(abortTime);
							job.setResultType(JobResultType.DID_NOT_COMPLETE);
						});
						logger.warn("Aborting jobs - committing transforms");
						int committed = Transaction.commit();
						if (committed == 0) {
							logger.warn(
									"Aborting jobs - no commits - don't reschedule wakeup");
						} else {
							logger.warn(
									"Aborting jobs - {} commits - reschedule wakeup",
									committed);
							fireWakeup();
						}
					});
		}
	}

	@Registration(
		value = ExecutionConstraints.class,
		implementation = Registration.Implementation.FACTORY)
	public static class DefaultExecutionConstraintsProvider
			implements RegistryFactory<ExecutionConstraints> {
		@Override
		public ExecutionConstraints impl() {
			return new ExecutionConstraints();
		}
	}

	@Registration(
		value = ResubmitPolicy.class,
		implementation = Registration.Implementation.FACTORY)
	public static class DefaultRetryPolicyProvider
			implements RegistryFactory<ResubmitPolicy> {
		@Override
		public ResubmitPolicy impl() {
			return new NoResubmitPolicy();
		}
	}

	public static class DefaultSchedule extends Schedule {
	}

	@Registration(
		value = Schedule.class,
		implementation = Registration.Implementation.FACTORY)
	public static class DefaultScheduleProvider
			implements RegistryFactory<Schedule> {
		@Override
		public Schedule impl() {
			return new Schedule();
		}
	}

	public static class ExecutionConstraints<T extends Task> {
		private static AdHocExecutorServiceProvider AD_HOC_INSTANCE = new AdHocExecutorServiceProvider();

		public static ExecutionConstraints forQueue(AllocationQueue queue) {
			return Registry.impl(ExecutionConstraints.class,
					queue.job.provideTaskClass()).withQueue(queue);
		}

		protected AllocationQueue queue;

		private ExecutorServiceProvider executorServiceProvider = AD_HOC_INSTANCE;

		private ExecutorServiceProvider descendantExecutorServiceProvider = AD_HOC_INSTANCE;

		public long calculateMaxAllocatable(AllocationQueue allocationQueue) {
			return Integer.MAX_VALUE;
		}

		public ExecutorServiceProvider getDescendantExcutorServiceProvider() {
			return descendantExecutorServiceProvider;
		}

		public ExecutorServiceProvider getExecutorServiceProvider() {
			if (queue.currentPhase == SubqueuePhase.Child) {
				return getDescendantExcutorServiceProvider();
			}
			return executorServiceProvider;
		}

		public boolean isClusteredChildAllocation() {
			return false;
		}

		public ExecutionConstraints withDescendantExecutorServiceProvider(
				ExecutorServiceProvider descendantExcutorServiceProvider) {
			this.descendantExecutorServiceProvider = descendantExcutorServiceProvider;
			return this;
		}

		public ExecutionConstraints withExecutorServiceProvider(
				ExecutorServiceProvider executorServiceProvider) {
			this.executorServiceProvider = executorServiceProvider;
			return this;
		}

		private ExecutionConstraints withQueue(AllocationQueue queue) {
			this.queue = queue;
			return this;
		}

		protected T provideTask() {
			return (T) queue.job.getTask();
		}

		private static class AdHocExecutorServiceProvider
				implements ExecutorServiceProvider {
			private AtomicInteger counter = new AtomicInteger();

			@Override
			public ExecutorService getService(AllocationQueue queue) {
				return Executors.newSingleThreadExecutor(
						new NamedThreadFactory("custom-name") {
							@Override
							public Thread newThread(Runnable r) {
								Thread thread = super.newThread(r);
								thread.setName("adhoc-executor-"
										+ counter.incrementAndGet());
								return thread;
							}
						});
			}

			@Override
			public void onServiceComplete(ExecutorService executorService) {
				executorService.shutdown();
			}
		}
	}

	public interface ExecutorServiceProvider {
		ExecutorService getService(AllocationQueue queue);

		default void onServiceComplete(ExecutorService executorService) {
		}
	}

	public static class NoResubmitPolicy extends ResubmitPolicy {
		@Override
		public boolean shouldResubmit(Job job) {
			return false;
		}
	}

	public static class ResubmitNTimesPolicy extends ResubmitPolicy {
		private int nTimes;

		public ResubmitNTimesPolicy(int nTimes) {
			this.nTimes = nTimes;
		}

		@Override
		public boolean shouldResubmit(Job orphanedJob) {
			if (!Ax.isTest()) {
				if (orphanedJob.getUser() != UserlandProvider.get()
						.getSystemUser()) {
					return false;
				}
			}
			int counter = 0;
			Job cursor = orphanedJob;
			while (true) {
				counter++;
				Optional<Job> precedingJob = cursor.getToRelations().stream()
						.filter(rel -> rel
								.getType() == JobRelationType.RESUBMIT)
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

	public abstract static class ResubmitPolicy<T extends Task> {
		public static ResubmitPolicy forJob(Job job) {
			return Registry.impl(ResubmitPolicy.class, job.provideTaskClass());
		}

		public static ResubmitPolicy retryNTimes(int nTimes) {
			return new ResubmitNTimesPolicy(nTimes);
		}

		public void visit(Job job) {
			if (shouldResubmit(job)) {
				resubmit(job);
			}
		}

		protected Logger logger() {
			return LoggerFactory.getLogger(getClass());
		}

		protected T provideTask(Job job) {
			return (T) job.getTask();
		}

		protected Job resubmit(Job job) {
			Job resubmit = JobRegistry.createBuilder().withTask(job.getTask())
					.withRelated(job).withRelationType(JobRelationType.RESUBMIT)
					.create();
			logger().warn("Resubmit job :: {} -> {})", job, resubmit);
			return resubmit;
		}

		protected boolean shouldResubmit(Job job) {
			return job.provideIsTopLevel() && job.getRunAt() != null
					&& job.getState().isResubmittable();
		}
	}

	@Registration(RetentionPolicy.class)
	public static class RetentionPolicy {
		public void delete(Job job) {
			logger().info("RetentionPolicy {} - deleting job {}",
					getClass().getSimpleName(), job);
			job.delete();
		}

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
			if (job.resolveState() == JobState.CANCELLED
					|| job.resolveState() == JobState.ABORTED) {
				return days <= 3;
			}
			return days < getRetentionDays();
		}

		protected int getRetentionDays() {
			return Configuration.getInt(JobScheduler.class,
					"defaultRetentionDays");
		}

		protected Logger logger() {
			return LoggerFactory.getLogger(getClass());
		}
	}

	/*
	 * Default schedule for scheduled classes. Note specifically that schedules
	 * are by default clustered, not vmLocal
	 */
	public static class Schedule {
		public static Schedule forTaskClass(Class<? extends Task> clazz) {
			return Registry.impl(Schedule.class, clazz);
		}

		private LocalDateTime next;

		private boolean fireOnApplicationStartup;

		private boolean vmLocal;

		public LocalDateTime getNext(boolean applicationStartup) {
			if (fireOnApplicationStartup && applicationStartup) {
				return LocalDateTime.now();
			}
			return next;
		}

		public boolean isCancelIfExistingIncomplete() {
			return true;
		}

		public boolean isVmLocal() {
			return this.vmLocal;
		}

		public Schedule
				withFireOnApplicationStartup(boolean fireOnApplicationStartup) {
			this.fireOnApplicationStartup = fireOnApplicationStartup;
			return this;
		}

		public Schedule withNext(LocalDateTime next) {
			this.next = next;
			return this;
		}

		public Schedule withVmLocal(boolean vmLocal) {
			this.vmLocal = vmLocal;
			return this;
		}
	}

	public class ScheduleJobsThread extends Thread {
		@Override
		public void run() {
			setName("Schedule-jobs-queue-"
					+ EntityLayerUtils.getLocalHostName());
			while (!finished) {
				try {
					ScheduleEvent event = events.take();
					/*
					 * We'll reach this point because a schedule event was fired
					 * during transform request processing. Wait until that
					 * event is completely processed (and visible to this
					 * thread/tx).
					 */
					environment.waitUntilCurrentRequestsProcessed();
					processEvent(event);
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					try {
						Transaction.ensureEnded();
					} catch (Exception e) {
						if (TransformManager.get() == null) {
							// shutting down
						} else {
							logger.warn(
									"DEVEX::0 - job scheduler cleanup exception",
									e);
						}
					}
				}
			}
		}
	}

	static class CurrentThreadExecutorServiceProvider
			implements ExecutorServiceProvider {
		@Override
		public ExecutorService getService(AllocationQueue queue) {
			return new CurrentThreadExecutorService();
		}
	}

	class ScheduleEvent {
		AllocationQueue.Event queueEvent;

		ScheduleEventType type;

		Transaction transaction;

		ScheduleEvent(AllocationQueue.Event queueEvent) {
			this.transaction = environment.getScheduleEventTransaction();
			this.queueEvent = queueEvent;
			this.type = ScheduleEventType.ALLOCATION_EVENT;
		}

		ScheduleEvent(ScheduleEventType type) {
			if (type != ScheduleEventType.SHUTDOWN) {
				this.transaction = environment.getScheduleEventTransaction();
			}
			this.type = type;
		}

		public boolean isSameAllocationQueueAs(ScheduleEvent event) {
			return event.queueEvent != null && queueEvent != null
					&& event.queueEvent.queue == queueEvent.queue
					&& event.transaction == transaction;
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToStringOneLine(this);
		}
	}

	enum ScheduleEventType {
		APPLICATION_STARTUP, WAKEUP, ALLOCATION_EVENT, FUTURE_CONSISTENCY_EVENT,
		SHUTDOWN, LEADER_CHANGED;

		public boolean isRefreshFuturesEvent() {
			switch (this) {
			case APPLICATION_STARTUP:
			case WAKEUP:
			case LEADER_CHANGED:
				return true;
			}
			return false;
		}
	}
}
