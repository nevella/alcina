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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.job.JobRelation.JobRelationType;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue.Event;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.SubqueuePhase;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.job.JobScheduler.ScheduleEvent.Type;

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
	private JobRegistry jobRegistry;

	private Timer timer = new Timer();

	private LocalDateTime nextScheduledWakeup = null;

	Logger logger = LoggerFactory.getLogger(getClass());

	private boolean finished;

	private ScheduleJobsThread thread;

	private BlockingQueue<ScheduleEvent> events = new LinkedBlockingQueue<>();

	private TopicListener<Event> queueEventListener = (k, v) -> events
			.add(new ScheduleEvent(v));

	Map<Job, JobAllocator> allocators = new ConcurrentHashMap<>();

	private ExecutorService allocatorService = Executors.newCachedThreadPool();

	JobScheduler(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		DomainDescriptorJob.get().queueEvents.add(queueEventListener);
		DomainDescriptorJob.get().fireInitialAllocatorQueueCreationEvents();
		thread = new ScheduleJobsThread();
		thread.start();
		// FIXME - mvcc.jobs.1a - this should instead listen on cluster change
		// events
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				fireWakeup();
			}
		}, 0L, 5 * TimeConstants.ONE_MINUTE_MS);
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
		return DomainDescriptorJob.get().getIncompleteJobs()
				.filter(job -> job.provideCreationDateOrNow().before(cutoff))
				.filter(job -> job.getCreator().toString()
						.matches(visibleInstanceRegex))
				.filter(job -> (job.getPerformer() == null
						&& !activeInstances.contains(job.getCreator()))
						|| (job.getPerformer() != null && !activeInstances
								.contains(job.getPerformer())));
	}

	public void stopService() {
		events.add(new ScheduleEvent(Type.SHUTDOWN));
	}

	private void futuresToPending() {
		if (!SchedulingPermissions.canFutureToPending()) {
			return;
		}
		DomainDescriptorJob.get().getAllFutureJobs()
				.filter(job -> job.getRunAt().compareTo(new Date()) <= 0)
				.filter(SchedulingPermissions::canModifyFuture).forEach(job -> {
					job.setPerformer(ClientInstance.self());
					job.setState(JobState.PENDING);
					logger.info("Job scheduler - future-to-pending - {}", job);
				});
	}

	private void processEvent(ScheduleEvent event) {
		MethodContext.instance().withWrappingTransaction()
				.withRootPermissions(true).run(() -> processEvent0(event));
	}

	private void processEvent0(ScheduleEvent event) {
		if (event.type.isRefreshFuturesEvent()) {
			/*
			 * orphans before scheduling (since we may resubmit
			 * timewise-constrained tasks)
			 */
			if (SchedulingPermissions.canProcessOrphans()) {
				processOrphans();
				Transaction.commit();
			}
			jobRegistry.withJobMetadataLock(
					getClass().getName() + "::futuresToPending", () -> {
						futuresToPending();
						refreshFutures(event);
						Transaction.commit();
					});
		}
		if (event.type == Type.SHUTDOWN) {
			// FIXME - mvcc.jobs.1a - shutdown all fixed pools
			allocators.values()
					.forEach(allocator -> allocator.fireDeletedEvent());
			timer.cancel();
			allocatorService.shutdown();
			finished = true;
		}
		if (event.type == Type.ALLOCATION_EVENT) {
			AllocationQueue queue = event.queueEvent.queue;
			boolean canAllocate = SchedulingPermissions.canAllocate(queue);
			if (canAllocate) {
				switch (event.queueEvent.type) {
				case CREATED: {
					JobAllocator allocator = new JobAllocator(queue,
							allocatorService);
					allocators.put(queue.job, allocator);
					allocator.enqueueEvent(event.queueEvent);
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
	}

	private void processOrphans() {
		if (jobRegistry.jobExecutors.isHighestBuildNumberInCluster()) {
			DomainDescriptorJob.get().getUndeserializableJobs()
					.forEach(Job::delete);
		}
		List<ClientInstance> activeInstances = jobRegistry.jobExecutors
				.getActiveServers();
		logger.info("Process orphans - visible instances: {}", activeInstances);
		/*
		 * handle flaky health/instances
		 */
		int minimumVisibleInstancesForOrphanProcessing = ResourceUtilities
				.getInteger(JobScheduler.class,
						"minimumVisibleInstancesForOrphanProcessing");
		if (activeInstances
				.size() < minimumVisibleInstancesForOrphanProcessing) {
			logger.info(
					"Not processing orphans - visible instances size: {}, minimum size: {}",
					activeInstances.size(),
					minimumVisibleInstancesForOrphanProcessing);
		}
		String visibleInstanceRegex = ResourceUtilities
				.get("visibleInstanceRegex");
		Date cutoff = SEUtilities
				.toOldDate(LocalDateTime.now().minusMinutes(1));
		AtomicBoolean zeroChanges = new AtomicBoolean(false);
		while (!zeroChanges.get() && getToAbortOrReassign(activeInstances,
				visibleInstanceRegex, cutoff).anyMatch(j -> true)) {
			jobRegistry.withJobMetadataLock(
					getClass().getName() + "::processOrphans", () -> {
						Stream<Job> doubleChecked = getToAbortOrReassign(
								activeInstances, visibleInstanceRegex, cutoff)
										.limit(1000);
						doubleChecked.forEach(job -> {
							logger.warn(
									"Aborting job {} (inactive client creator: {} - performer: {})",
									job, job.getCreator(), job.getPerformer());
							if (ResourceUtilities.is("abortDisabled")) {
								logger.warn(
										"(Would abort job - but abortDisabled)");
								return;
							}
							/* resubmit, then abort */
							ResubmitPolicy policy = ResubmitPolicy.forJob(job);
							policy.visit(job);
							job.setState(JobState.ABORTED);
							job.setEndTime(new Date());
							job.setResultType(JobResultType.DID_NOT_COMPLETE);
						});
						logger.warn("Aborting jobs - committing transforms");
						int committed = Transaction.commit();
						if (committed == 0) {
							zeroChanges.set(true);
						}
					});
		}
	}

	private void refreshFutures(ScheduleEvent event) {
		List<Class<? extends Task>> scheduleTaskClasses = (List) Registry.get()
				.allImplementationKeys(Schedule.class);
		LocalDateTime wakeup = LocalDateTime.now().plusYears(1);
		for (Class<? extends Task> key : scheduleTaskClasses) {
			Schedule schedule = Schedule.forTaskClass(key);
			LocalDateTime next = schedule
					.getNext(event.type == Type.APPLICATION_STARTUP);
			if (next == null) {
				continue;
			}
			Optional<Job> earliestFuture = DomainDescriptorJob.get()
					.earliestFuture(key, schedule.isVmLocal());
			if (earliestFuture.isPresent()) {
				Date nextDate = SEUtilities.toOldDate(next);
				if (earliestFuture.get().getRunAt().after(nextDate)) {
					if (SchedulingPermissions
							.canModifyFuture(earliestFuture.get())) {
						logger.info("Changed next run of {} to {}",
								earliestFuture.get(), next);
						earliestFuture.get().setRunAt(nextDate);
					}
					wakeup = next;
				} else {
					if (SchedulingPermissions.canFutureToPending()) {
						wakeup = SEUtilities.toLocalDateTime(
								earliestFuture.get().getRunAt());
					}
				}
			} else {
				Job test = AlcinaPersistentEntityImpl
						.getNewImplementationInstance(Job.class);
				test.setTask(Reflections.newInstance(key));
				if (SchedulingPermissions.canCreateFuture(schedule)) {
					Job job = JobRegistry.createBuilder()
							.withTask(Reflections.newInstance(key))
							.withRunAt(next).create();
					logger.info("Schedule new future job - {} to {}", job,
							next);
				}
			}
			if (next.isBefore(wakeup)) {
				wakeup = next;
			}
		}
		if (nextScheduledWakeup == null
				|| wakeup.isBefore(nextScheduledWakeup)) {
			if (wakeup.isBefore(LocalDateTime.now())) {
				events.add(new ScheduleEvent(Type.WAKEUP));
			} else {
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						fireWakeup();
					}
				}, SEUtilities.toOldDate(wakeup));
			}
			nextScheduledWakeup = wakeup;
		}
	}

	void enqueueLeaderChangedEvent() {
		MethodContext.instance().withWrappingTransaction()
				.call(() -> events.add(new ScheduleEvent(Type.LEADER_CHANGED)));
	}

	void fireWakeup() {
		MethodContext.instance().withWrappingTransaction()
				.run(() -> events.add(new ScheduleEvent(Type.WAKEUP)));
	}

	@RegistryLocation(registryPoint = ExecutionConstraints.class, implementationType = ImplementationType.FACTORY)
	public static class DefaultExecutionConstraintsProvider
			implements RegistryFactory<ExecutionConstraints> {
		@Override
		public ExecutionConstraints impl() {
			return new ExecutionConstraints();
		}
	}

	@RegistryLocation(registryPoint = ResubmitPolicy.class, implementationType = ImplementationType.FACTORY)
	public static class DefaultRetryPolicyProvider
			implements RegistryFactory<ResubmitPolicy> {
		@Override
		public ResubmitPolicy impl() {
			return new NoRetryPolicy();
		}
	}

	@RegistryLocation(registryPoint = Schedule.class, implementationType = ImplementationType.INSTANCE)
	public static class DefaultSchedule extends Schedule {
	}

	@RegistryLocation(registryPoint = Schedule.class, implementationType = ImplementationType.FACTORY)
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

		public long calculateMaxAllocatable() {
			return Integer.MAX_VALUE;
		}

		public ExecutorServiceProvider getDescendantExcutorServiceProvider() {
			return descendantExecutorServiceProvider;
		}

		public ExecutorServiceProvider getExecutorServiceProvider() {
			if (queue.phase == SubqueuePhase.Child) {
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
		}
	}

	public interface ExecutorServiceProvider {
		ExecutorService getService(AllocationQueue queue);
	}

	public static class NoRetryPolicy extends ResubmitPolicy {
		@Override
		public boolean shouldResubmit(Job job) {
			return false;
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
					.withRunAt(SEUtilities.toLocalDateTime(job.getRunAt()))
					.create();
			logger().warn("Resubmit job :: {} -> {})", job, resubmit);
			return resubmit;
		}

		protected boolean shouldResubmit(Job job) {
			return job.provideIsTopLevel() && job.getRunAt() != null
					&& job.getState().isResubmittable();
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
					DomainStore.waitUntilCurrentRequestsProcessed();
					processEvent(event);
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					Transaction.end();
				}
			}
		}
	}

	private static class ResubmitNTimesPolicy extends ResubmitPolicy {
		private int nTimes;

		public ResubmitNTimesPolicy(int nTimes) {
			this.nTimes = nTimes;
		}

		@Override
		public boolean shouldResubmit(Job failedJob) {
			if (failedJob.getUser() != UserlandProvider.get().getSystemUser()) {
				return false;
			}
			int counter = 0;
			Job cursor = failedJob;
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

	static class CurrentThreadExecutorServiceProvider
			implements ExecutorServiceProvider {
		@Override
		public ExecutorService getService(AllocationQueue queue) {
			return new CurrentThreadExecutorService();
		}
	}

	static class ScheduleEvent {
		AllocationQueue.Event queueEvent;

		Type type;

		Transaction transaction;

		ScheduleEvent(AllocationQueue.Event queueEvent) {
			this.transaction = Transaction.current();
			Preconditions.checkNotNull(this.transaction);
			this.queueEvent = queueEvent;
			this.type = Type.ALLOCATION_EVENT;
		}

		ScheduleEvent(Type type) {
			if (type != Type.SHUTDOWN) {
				this.transaction = Transaction.current();
				Preconditions.checkNotNull(this.transaction);
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

		enum Type {
			APPLICATION_STARTUP, WAKEUP, ALLOCATION_EVENT, SHUTDOWN,
			LEADER_CHANGED;
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
}
