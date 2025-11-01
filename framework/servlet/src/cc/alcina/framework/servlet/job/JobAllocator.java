package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.domain.TransactionEnvironment;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.LazyLoadProvideTask;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.Event;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.EventType;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.SubqueuePhase;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.servlet.job.JobRegistry.LatchType;
import cc.alcina.framework.servlet.job.JobRegistry.LauncherThreadState;
import cc.alcina.framework.servlet.job.JobScheduler.ExceptionPolicy;
import cc.alcina.framework.servlet.job.JobScheduler.ExceptionPolicy.AbortReason;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutorServiceProvider;

/*
 * Start the wrapped thread either on creation - if a 'self-starter' (top-level,
 * first in sequence), or once the job has reached stage 'processing'
 * 
 * TODO - childcompletionlatch is only counted down if there's a subsequent -
 * but I'm not sure of the locking there.
 * 
 * Possibly childcompletionlatch should not be used, rather an atomicint
 * (counter) of children - or some other *domain* structure. Workaround is the
 * backup recalc during the loop
 */
class JobAllocator {
	private static void commit() {
		TransactionEnvironment.get().commitWithBackoff();
	}

	private AllocationQueue queue;

	private volatile AllocationTask allocationTask;

	private BlockingQueue<AllocationQueue.Event> eventQueue = new LinkedBlockingQueue<>();

	private CountDownLatch childCompletionLatch = new CountDownLatch(1);

	private CountDownLatch sequenceCompletionLatch = new CountDownLatch(1);

	private StatusMessage lastStatus;

	static Logger logger = LoggerFactory.getLogger(JobAllocator.class);

	private ExecutorService allocatorService;

	private volatile boolean finished = false;

	private JobContext jobContext;

	private StatusMessage enqueuedStatusMessage;

	Thread thread;

	// locks actions which require lock over the whole processEvent0
	private Object addSubsequentsMonitor = new Object();

	private volatile Job awaitJobExistenceBeforeContinueToExit;

	private boolean beforeAddSubsequentsBarrier = true;

	JobAllocator(AllocationQueue queue, ExecutorService allocatorService) {
		this.queue = queue;
		this.allocatorService = allocatorService;
		queue.events.add(this::enqueueEvent);
		lastStatus = new StatusMessage();
		/*
		 * Allocator threads are started for top-level jobs iff visible:
		 */
		Job job = queue.job;
		new JobObservable.AllocatorCreated(job).publish();
		boolean topLevelQueue = job.provideIsTopLevel()
				&& job.provideIsFirstInSequence();
		/*
		 * Top-level jobs will always have a performer set at this point -
		 * either on creation or via future-to-pending
		 */
		boolean visible = job.getPerformer() == ClientInstance.current()
				|| (ExecutionConstraints.forQueue(queue)
						.isClusteredChildAllocation()
						&& JobRegistry.isActiveInstance(job.getCreator()));
		if (topLevelQueue && visible) {
			ensureStarted();
		}
	}

	public void applyStatusMessage() {
		if (enqueuedStatusMessage == null) {
			return;
		}
		TransactionEnvironment.get().ensureBegun();
		queue.job.setStatusMessage(enqueuedStatusMessage.message);
		queue.job.setCompletion(enqueuedStatusMessage.percentComplete / 100.0);
		commit();
		enqueuedStatusMessage = null;
	}

	/*
	 * Called only from the performer thread (so job.provideChildren is live)
	 * 
	 * Once entered, subsequent jobs cannot be added to this job
	 */
	void awaitChildCompletion(JobContext jobContext) {
		this.jobContext = jobContext;
		try {
			checkAddSubsequentsBarrier();
			if (queue.job.provideChildren().noneMatch(j -> true)) {
				return;
			}
			TransactionEnvironment.get().ensureEnded();
			ensureStarted();
			boolean wasCheckedComplete = false;
			while (!childCompletionLatch.await(2, TimeUnit.SECONDS)) {
				TransactionEnvironment.get().endAndBeginNew();
				if (enqueuedStatusMessage != null) {
					TransactionEnvironment.withDomain(this::applyStatusMessage);
				}
				Boolean checkedComplete = TransactionEnvironment
						.withDomain(this::doubleCheckChildCompletion);
				wasCheckedComplete |= checkedComplete;
				TransactionEnvironment.get().end();
			}
			/*
			 * interesting - possibly due to queueing on LDT?
			 */
			if (wasCheckedComplete) {
				int debug = 4;
			}
			TransactionEnvironment.get().endAndBeginNew();
			new StatusMessage().publish();
			TransactionEnvironment.withDomain(this::applyStatusMessage);
			TransactionEnvironment.get().ensureBegun();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void checkAddSubsequentsBarrier() throws InterruptedException {
		synchronized (addSubsequentsMonitor) {
			if (awaitJobExistenceBeforeContinueToExit != null) {
				/*
				 * spinlock with timeout%/
				 */
				long start = System.currentTimeMillis();
				while (TimeConstants.within(start,
						TimeConstants.ONE_MINUTE_MS)) {
					logger.info("await spinlock - {} -  job {}",
							queue.currentPhase,
							awaitJobExistenceBeforeContinueToExit.toLocator());
					Job domainVisible = LazyLoadProvideTask.CONTEXT_LAZY_LOAD_DISABLED
							.callWithTrue(
									() -> awaitJobExistenceBeforeContinueToExit
											.toLocator().find());
					/*
					 * a spinlock is incorrect if in a single-threaded tx
					 * environment
					 */
					if (!TransactionEnvironment.get().isMultiple()
							|| (domainVisible != null && JobRegistry.get()
									.hasAllocator(domainVisible))) {
						awaitJobExistenceBeforeContinueToExit = null;
						Transaction.endAndBeginNew();
						break;
					} else {
						Thread.sleep(10);
						Transaction.endAndBeginNew();
					}
				}
			}
			beforeAddSubsequentsBarrier = false;
		}
	}

	public void awaitSequenceCompletion() {
		ensureStarted();
		try {
			JobRegistry.awaitLatch(queue.job, sequenceCompletionLatch,
					LatchType.SEQUENCE_COMPLETION);
		} catch (Exception e) {
			logger.warn("DEVEX-0 -- job sequence timeout/interruption", e);
		}
	}

	public void enqueueEvent(Event queueEvent) {
		eventQueue.add(queueEvent);
	}

	void ensureStarted() {
		AllocationTask createdTask = null;
		synchronized (this) {
			if (allocationTask == null) {
				allocationTask = new AllocationTask();
				createdTask = allocationTask;
			}
		}
		if (createdTask != null) {
			allocatorService.execute(allocationTask);
		}
	}

	private JobEnvironment environment() {
		return JobRegistry.get().getEnvironment();
	}

	public void fireDeletedEvent() {
		queue.publish(EventType.DELETED);
	}

	ExecutionConstraints getExecutionConstraints() {
		return ExecutionConstraints.forQueue(queue);
	}

	void onFinished() {
		finished = true;
		try {
			new StatusMessage().publish();
		} catch (Exception e) {
			// to handle the job-deleted case
			e.printStackTrace();
		}
		// this is essentially job-nuking
		childCompletionLatch.countDown();
		sequenceCompletionLatch.countDown();
	}

	void toAwaitingChildren(JobContext jobContext) {
		this.jobContext = jobContext;
		if (queue.currentPhase == SubqueuePhase.Self) {
			queue.publish(EventType.TO_AWAITING_CHILDREN);
		}
	}

	@Override
	public String toString() {
		return queue.toString();
	}

	private class AllocationTask implements Runnable {
		boolean firstEvent = true;

		long lastAllocated = System.currentTimeMillis();

		boolean isAllocatable(Job job) {
			switch (queue.currentPhase) {
			case Self:
				if (!SchedulingPermissions.canAllocate(queue.job)) {
					return false;
				}
				/*
				 * only top-level, first-in-sequence jobs are allocated by their
				 * own allocator
				 */
				return job.provideIsTopLevel()
						&& job.provideIsFirstInSequence();
			case Child:
				// the queue will only be visible if either local or clustered
				// child allocation, so no need to check this
				// return ExecutionConstraints.forQueue(queue)
				// .isClusteredChildAllocation();
				/*
				 * Also, no jobs visible in this phase will have sequential
				 * predecessors
				 *
				 */
				return true;
			case Sequence:
				if (job.providePreviousOrSelfInSequence() == job) {
					/*
					 * A bug - this question shouldn't be aksed of
					 * first-in-sequence - probable issue with ddj/remove
					 */
					return false;
				}
				return job.providePreviousOrSelfInSequence().provideIsComplete()
						&& job.providePreviousOrSelfInSequence()
								.getPerformer() == ClientInstance.current();
			default:
				throw new UnsupportedOperationException();
			}
		}

		private boolean isPhaseComplete(Event event) {
			if (event.type == EventType.DELETED) {
				return true;
			}
			boolean selfPerformer = queue.job.getPerformer() == ClientInstance
					.current();
			switch (queue.currentPhase) {
			case Self:
				if (queue.job.provideIsComplete()) {
					return true;
				}
				if (queue.job.resolveState() == JobState.PROCESSING) {
					if (selfPerformer) {
						// handled by jobContext and/or the jobPerformer
						return event.type == EventType.TO_AWAITING_CHILDREN;
					} else {
						return true;
					}
				} else {
					return false;
				}
			case Child:
				if (queue.job.provideIsComplete()) {
					return true;
				}
				// non-self performers may reach this phase before children are
				// populated, self guaranteed not to
				if (selfPerformer) {
					return queue.isNoPendingJobsInPhase();
				} else {
					return queue.job.provideIsComplete();
				}
			case Sequence:
				return queue.isNoPendingJobsInPhase();
			case Complete:
				return false;
			default:
				throw new UnsupportedOperationException();
			}
		}

		public void processAllocationEvent(Event event) {
			try {
				TransactionEnvironment.get().begin();
				if (event == null) {
					/*
					 * TODO - probably remove this - it tends to only get hit
					 * while awaiting child jobs which are themselves awaiting
					 * sequence completion - which means it does nothing
					 */
					int debug = 3;
					// missed event?
					queue.publish(EventType.WAKEUP);
				} else {
					TransactionEnvironment.get()
							.waitUntilCurrentRequestsProcessed();
					processEvent(event);
					/*
					 * Only need to process the first RELATED_MODIFICATION
					 */
					if (event.type == EventType.RELATED_MODIFICATION) {
						Event peekEvent = null;
						while ((peekEvent = eventQueue.peek()) != null
								&& peekEvent.transactionId == event.transactionId
								&& peekEvent.type == EventType.RELATED_MODIFICATION) {
							eventQueue.poll();
						}
					}
					commit();
				}
			} catch (Exception e) {
				logger.warn("Exception in allocator");
				logger.warn("Trace: ", e);
				e.printStackTrace();
			} finally {
				TransactionEnvironment.get().ensureEnded();
			}
		}

		void processEvent(Event event) {
			if (finished) {
				return;
			}
			try {
				TransactionEnvironment.get().ensureBegun();
				processEvent0(event);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				TransactionEnvironment.get().ensureEnded();
			}
		}

		void processEvent0(Event event) throws Exception {
			Job job = queue.job;
			if (firstEvent) {
				firstEvent = false;
				environment().setAllocatorThreadName(
						Ax.format("job-allocator::%s", queue.toDisplayName()));
				logger.debug("Allocation thread started -  job {}",
						job.toDisplayName());
			}
			if (awaitJobExistenceBeforeContinueToExit != null) {
				/*
				 * spinlock with timeout - the actual change occurs on the
				 * performer thread
				 */
				long start = System.currentTimeMillis();
				while (TimeConstants.within(start,
						TimeConstants.ONE_MINUTE_MS)) {
					if (awaitJobExistenceBeforeContinueToExit == null ||
					/*
					 * for a non-multiple-tx env - spinlock would not work
					 */
							!TransactionEnvironment.get().isMultiple()) {
						Transaction.endAndBeginNew();
						break;
					} else {
						Thread.sleep(10);
					}
				}
			}
			boolean deleted = false;
			try {
				LooseContext.push();
				LazyLoadProvideTask.CONTEXT_LAZY_LOAD_DISABLED.setTrue();
				if (job.domain().wasRemoved()) {
					// production issue -- revisit
					Thread.sleep(1000);
					DomainStore.waitUntilCurrentRequestsProcessed();
					if (!job.domain().wasRemoved()) {
						logger.debug(
								"DEVEX-12 ::  event with incomplete domain tx -  job {}",
								job.toDisplayName());
					} else {
						deleted = true;
						// was deleted - FIXME - mvcc.jobs.2 - remove this -
						// improve upstream
						// (AllocationQueue insert/remove)
					}
				}
			} finally {
				LooseContext.pop();
			}
			if (!deleted) {
				new StatusMessage().checkPublish();
			}
			if (queue.currentPhase == SubqueuePhase.Complete
					|| job.resolveState() == JobState.CANCELLED
					|| job.resolveState() == JobState.ABORTED
					/*
					 * sneaky deletion - FIXME - mvcc.jobs.2
					 */
					|| deleted) {
				logger.debug("Allocation thread ended -  job {}",
						job.toDisplayName());
				logger.debug(
						"Allocation thread debug -  job {} - phase {} - state {} - selfPerformer {} - sequential {} - deleted {}",
						job.getId(), queue.currentPhase, job.getState(),
						job.getPerformer() == ClientInstance.current(),
						job.provideRelatedSequential().size(), deleted);
				if (queue.currentPhase == SubqueuePhase.Complete) {
					if (job.getState() == JobState.COMPLETED
							&& job.getPerformer() == ClientInstance.current()
							&& job.provideRelatedSequential().size() > 1
							&& !deleted) {
						job.setState(JobState.SEQUENCE_COMPLETE);
						commit();
					}
				}
				// don't remove directly - breaks deallocation - rely on
				// jobprojection to do this correctly
				// JobDomain.get().removeAllocationQueue(job);
				onFinished();
				return;
			}
			if (event.type == EventType.WAKEUP) {
			} else {
				logger.debug("Allocation thread - job {} - event {}",
						job.toDisplayName(), event);
			}
			if (isPhaseComplete(event)) {
				if (queue.currentPhase == SubqueuePhase.Child) {
					/*
					 * only countdown the child latch if we'll be waiting on
					 * sequence
					 */
					if (queue.job.provideNextInSequence().isPresent()) {
						logger.debug(
								"Releasing child completion latch - job {}",
								job.toDisplayName());
						childCompletionLatch.countDown();
					}
					/*
					 * correction - only if *children* are present...?
					 * 
					 */
					// if (queue.job.provideChildren().count() > 0) {
					// logger.debug(
					// "Releasing child completion latch - job {}",
					// job.toDisplayName());
					// childCompletionLatch.countDown();
					// }
				}
				SubqueuePhase priorPhase = queue.currentPhase;
				queue.incrementPhase();
				logger.debug("Changed phase :: {} :: {} -> {}",
						job.toDisplayName(), priorPhase, queue.currentPhase);
				// resubmit until event does not cause phase change
				// (or complete)
				new StatusMessage().publish();
				enqueueEvent(event);
			} else {
				TransactionEnvironment.get().endAndBeginNew();
				boolean useParentConstraints = (queue.currentPhase == SubqueuePhase.Sequence
						|| queue.currentPhase == SubqueuePhase.Child)
						&& queue.job.provideParent().isPresent();
				AllocationQueue constraintQueue = useParentConstraints
						? queue.ensureParentQueue()
						: queue;
				ExecutionConstraints executionConstraints = ExecutionConstraints
						.forQueue(constraintQueue);
				long maxAllocatable = queue.currentPhase == SubqueuePhase.Sequence
						? 1// essentially passing the allocation
							// slot to the next-in-sequence
						: executionConstraints
								.calculateMaxAllocatable(constraintQueue);
				Optional<Job> resubmittedFrom = queue.job
						.provideResubmittedFrom();
				if (resubmittedFrom.isPresent()
						&& !resubmittedFrom.get().provideIsComplete()) {
					maxAllocatable = 0;
				}
				// FIXME - mvcc.jobs.1a - allocate in batches (i.e.
				// 30...let drain to 10...again)
				if (maxAllocatable > 0) {
					long incompleteAllocated = queue
							.getIncompleteAllocatedJobCountForCurrentPhaseThisVm();
					// FIXME - incompleteAllocated should never approach 30 -
					// but it does...
					if (incompleteAllocated < 30 && queue.getUnallocatedJobs()
							.anyMatch(this::isAllocatable)) {
						ExecutorServiceProvider executorServiceProvider = executionConstraints
								.getExecutorServiceProvider();
						ExecutorService executorService = executorServiceProvider
								.getService(constraintQueue);
						List<Job> allocating = new ArrayList<>();
						Runnable allocateJobs = () -> {
							/*
							 * Double-checking (inside lock). No need to check
							 * for a prior incomplete resubmit job here
							 */
							long maxAllocatableLocked = queue.currentPhase == SubqueuePhase.Sequence
									? 1// essentially passing the allocation
										// slot to the next-in-sequence
									: executionConstraints
											.calculateMaxAllocatable(
													constraintQueue);
							/*
							 * Double-checking
							 */
							Set<Job> invalidAllocated = new LinkedHashSet<>();
							queue.getUnallocatedJobs()
									.filter(this::isAllocatable)
									.limit(maxAllocatableLocked)
									.forEach(allocating::add);
							allocating.forEach(j -> {
								if (j.getState() != JobState.PENDING) {
									logger.warn(
											"jobAllocator-invalid-state - not allocating job {}",
											j);
									List<Job> maTest = queue
											.getUnallocatedJobs()
											.filter(this::isAllocatable)
											.limit(maxAllocatableLocked)
											.collect(Collectors.toList());
									invalidAllocated.add(j);
								} else {
									j.setState(JobState.ALLOCATED);
									j.setPerformer(ClientInstance.current());
									logger.debug(
											"Allocated job {} - queue {}/{}", j,
											queue.job, queue.currentPhase);
									lastAllocated = System.currentTimeMillis();
								}
							});
							/*
							 * If this fails, we *want* it to throw an exception
							 * (for later retry)
							 */
							commit();
							allocating.forEach(j -> logger.debug(
									"Sending to executor service - {} - {}",
									j.getId(), j));
							allocating.stream()
									.filter(j -> !invalidAllocated.contains(j))
									.forEach(j -> {
										JobContext existingContext = JobRegistry
												.get().getContext(j);
										if (existingContext != null) {
											logger.warn(
													"jobAllocator-invalid-state - not allocating job {} - existing context; launcher was {}",
													j,
													existingContext.launcherThreadState);
										} else {
											LauncherThreadState launcherThreadState = new LauncherThreadState();
											logger.debug(
													"Sending to executor service (2) - {} - {}",
													j.getId(), j);
											executorService
													.submit(() -> JobRegistry
															.get().performJob(j,
																	false,
																	launcherThreadState,
																	executorServiceProvider,
																	executorService));
										}
									});
						};
						// FIXME - mvcc.jobs.1a - getting splurgey allocation?
						/*
						 * FIMXE followup - the above can probably go once we
						 * have a formal description of allocation behaviour
						 */
						String lockPath = executionConstraints
								.getAllocationLockPath(job);
						Object lock = JobRegistry.get()
								.withJobMetadataLock(lockPath, allocateJobs);
						if (lock == null) {
							logger.warn("Ran allocation job without lock? {}",
									job);
						}
					}
				}
			}
			long timeSinceAllocation = System.currentTimeMillis()
					- lastAllocated;
			ExceptionPolicy exceptionPolicy = ExceptionPolicy.forJob(job);
			long allocationTimeout = exceptionPolicy.getAllocationTimeout(job);
			if (timeSinceAllocation > allocationTimeout && jobContext != null
					&& (jobContext.getJob().getPerformer() == null
							// FIXME - jobs - performer should never be null at
							// this point
							|| jobContext.getJob()
									.getPerformer() == ClientInstance.current())
					&& Configuration.is(Transactions.class,
							"cancelTimedoutTransactions")) {
				List<Job> incompleteChildren = job.provideChildren()
						.filter(j -> j.provideIsNotComplete()
								|| j.getState() == JobState.COMPLETED)
						.collect(Collectors.toList());
				logger.warn(
						"DEVEX::0 - Cancelling/aborting timed-out job - no allocations for {} ms :: {} - incomplete children :: {}",
						allocationTimeout, job, incompleteChildren);
				Stream<Job> toAbort = incompleteChildren.isEmpty()
						|| exceptionPolicy.isAbortParentOnChildTimeout()
								? Stream.of(job)
								: incompleteChildren.stream();
				Stream<Job> toCancel = incompleteChildren.isEmpty()
						? Stream.empty()
						: Stream.of(job);
				toAbort.forEach(j -> {
					ExceptionPolicy policy = ExceptionPolicy.forJob(j);
					policy.onBeforeAbort(j, AbortReason.TIMED_OUT);
					j.setState(JobState.ABORTED);
					j.setEndTime(new Date());
					j.setResultType(JobResultType.DID_NOT_COMPLETE);
					commit();
				});
				toCancel.forEach(Job::cancel);
				commit();
			}
			commit();
		}

		@Override
		public void run() {
			thread = Thread.currentThread();
			new JobObservable.AllocationThreadStarted(queue.job).publish();
			try {
				Permissions.pushSystemUser();
				while (!finished) {
					try {
						Event event = eventQueue.poll(1, TimeUnit.SECONDS);
						environment().runInTransactionThread(
								() -> processAllocationEvent(event));
					} catch (Exception e) {
						logger.warn("Exception in allocator (outer)");
						logger.warn("Trace: ", e);
						e.printStackTrace();
					}
				}
			} finally {
				Permissions.popContext();
			}
			environment().setAllocatorThreadName("job-allocator::idle");
		}
	}

	class StatusMessage {
		long publishTime;

		int percentComplete;

		long completedCount;

		long totalCount;

		String message;

		SubqueuePhase phase;

		public StatusMessage() {
			phase = queue.currentPhase;
			completedCount = queue.getCompletedJobCount();
			totalCount = queue.getTotalJobCount();
			if (finished && !queue.job.provideNextInSequence().isPresent()
					&& queue.job.getState() == JobState.PROCESSING) {
				long childCount = queue.job.provideChildren().count();
				if (childCount > 0) {
					completedCount = childCount;
					totalCount = childCount;
				} else {
					completedCount++;
					if (totalCount == 0) {
						totalCount = 1;
					}
				}
			}
			percentComplete = (int) (((double) completedCount) / totalCount
					* 100.0);
			publishTime = System.currentTimeMillis();
		}

		public void checkPublish() {
			if (shouldPublish()) {
				publish();
			}
		}

		public void publish() {
			if (jobContext == null) {
				return;
			}
			lastStatus = this;
			message = Ax.format("%s - %s% (%s/%s)", Ax.friendly(phase),
					percentComplete, completedCount, totalCount);
			enqueuedStatusMessage = this;
			if (queue.currentPhase == SubqueuePhase.Sequence
					&& queue.job.getPerformer() == ClientInstance.current()
					&& jobContext == null) {
				applyStatusMessage();
				commit();
			}
		}

		public boolean shouldPublish() {
			if (phase == SubqueuePhase.Self) {
				return false;
			}
			if (phase != lastStatus.phase) {
				return true;
			}
			if (completedCount == lastStatus.completedCount
					&& totalCount == lastStatus.totalCount) {
				return phase == lastStatus.phase;
			}
			if (percentComplete == lastStatus.percentComplete) {
				return publishTime - lastStatus.publishTime > 2
						* TimeConstants.ONE_SECOND_MS;
			}
			return true;
		}
	}

	boolean doubleCheckChildCompletion() {
		if (queue.job.provideChildren().count() > 0
				&& queue.job.provideChildrenAndChildSubsequents()
						.allMatch(Job::provideIsComplete)) {
			logger.info("children are complete, but waiting on lock - {}",
					queue.job);
			// Ax.err("DEVEX-0 -- Marking as children complete - latch issue -
			// %s",
			// queue.job);
			// Ax.out(queue.job.provideChildrenAndChildSubsequents()
			// .collect(Collectors.toList()));
			return true;
			// childCompletionLatch.countDown();
		} else {
			return false;
		}
	}

	/*
	 * locking logic (fix for job/2) - queue.currentPhase is only changed within
	 * the other section (processEvent0) locked by processEventMonitor
	 */
	boolean setAwaitJobExistenceBeforeContinueToExit(Job job) {
		synchronized (addSubsequentsMonitor) {
			if (beforeAddSubsequentsBarrier) {
				awaitJobExistenceBeforeContinueToExit = job;
				return true;
			}
		}
		return false;
	}
}
