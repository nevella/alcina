package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobResultType;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.LazyLoadProvideTask;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.Event;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.EventType;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.SubqueuePhase;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.servlet.job.JobRegistry.LauncherThreadState;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;
import cc.alcina.framework.servlet.job.JobScheduler.ResubmitPolicy;

/*
 * Start the wrapped thread either on creation - if a 'self-starter' (top-level, first in sequence), or once the job has reached stage 'processing'
 */
class JobAllocator {
	private AllocationQueue queue;

	private AllocationTask allocationTask;

	private BlockingQueue<AllocationQueue.Event> eventQueue = new LinkedBlockingQueue<>();

	private CountDownLatch childCompletionLatch = new CountDownLatch(1);

	private CountDownLatch sequenceCompletionLatch = new CountDownLatch(1);

	private StatusMessage lastStatus;

	Logger logger = LoggerFactory.getLogger(getClass());

	private ExecutorService allocatorService;

	private volatile boolean finished = false;

	private JobContext jobContext;

	private StatusMessage enqueuedStatusMessage;

	Thread thread;

	JobAllocator(AllocationQueue queue, ExecutorService allocatorService) {
		this.queue = queue;
		this.allocatorService = allocatorService;
		queue.events.add((k, e) -> enqueueEvent(e));
		lastStatus = new StatusMessage();
		/*
		 * Allocator threads are started for top-level jobs iff visible:
		 */
		Job job = queue.job;
		boolean topLevelQueue = job.provideIsTopLevel()
				&& job.provideIsFirstInSequence();
		/*
		 * Top-level jobs will always have a performer set at this point -
		 * either on creation or via future-to-pending
		 */
		boolean visible = job.getPerformer() == ClientInstance.self()
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
		Transaction.ensureBegun();
		queue.job.setStatusMessage(enqueuedStatusMessage.message);
		queue.job.setCompletion(enqueuedStatusMessage.percentComplete / 100.0);
		Transaction.commit();
		enqueuedStatusMessage = null;
	}

	public void awaitSequenceCompletion() {
		ensureStarted();
		try {
			while (!sequenceCompletionLatch.await(2, TimeUnit.SECONDS)) {
				int debug = 3;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void enqueueEvent(Event queueEvent) {
		eventQueue.add(queueEvent);
	}

	public void fireDeletedEvent() {
		queue.publish(EventType.DELETED);
	}

	@Override
	public String toString() {
		return queue.toString();
	}

	/*
	 * Called only from the performer thread (so job.provideChildren is live)
	 */
	void awaitChildCompletion(JobContext jobContext) {
		this.jobContext = jobContext;
		try {
			if (queue.job.provideChildren().noneMatch(j -> true)) {
				return;
			}
			Transaction.ensureEnded();
			ensureStarted();
			while (!childCompletionLatch.await(2, TimeUnit.SECONDS)) {
				if (enqueuedStatusMessage != null) {
					applyStatusMessage();
					Transaction.end();
				}
			}
			Transaction.endAndBeginNew();
			new StatusMessage().publish();
			applyStatusMessage();
			Transaction.ensureBegun();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void ensureStarted() {
		if (allocationTask == null) {
			allocationTask = new AllocationTask();
			allocatorService.execute(allocationTask);
		}
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
		childCompletionLatch.countDown();
		sequenceCompletionLatch.countDown();
	}

	void toAwaitingChildren() {
		if (queue.currentPhase == SubqueuePhase.Self) {
			queue.publish(EventType.TO_AWAITING_CHILDREN);
		}
	}

	private class AllocationTask implements Runnable {
		boolean firstEvent = true;

		long lastAllocated = System.currentTimeMillis();

		public void processEvent(Event event) {
			if (finished) {
				return;
			}
			Transaction.ensureBegun();
			Job job = queue.job;
			if (firstEvent) {
				firstEvent = false;
				Thread.currentThread().setName(
						Ax.format("job-allocator::%s", queue.toDisplayName()));
				logger.debug("Allocation thread started -  job {}",
						job.toDisplayName());
			}
			boolean deleted = false;
			try {
				LooseContext.pushWithTrue(
						LazyLoadProvideTask.CONTEXT_LAZY_LOAD_DISABLED);
				if (job.domain().domainVersion() == null) {
					try {
						// production issue -- revisit
						Thread.sleep(1000);
						DomainStore.waitUntilCurrentRequestsProcessed();
						if (job.domain().domainVersion() != null) {
							logger.debug(
									"DEVEX-12 ::  event with incomplete domain tx -  job {}",
									job.toDisplayName());
						} else {
							deleted = true;
							// was deleted - FIXME - mvcc.jobs.2 - remove this -
							// improve upstream
							// (AllocationQueue insert/remove)
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
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
						job.getPerformer() == ClientInstance.self(),
						job.provideRelatedSequential().size(), deleted);
				if (queue.currentPhase == SubqueuePhase.Complete) {
					if (job.getState() == JobState.COMPLETED
							&& job.getPerformer() == ClientInstance.self()
							&& job.provideRelatedSequential().size() > 1
							&& !deleted) {
						job.setState(JobState.SEQUENCE_COMPLETE);
						Transaction.commit();
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
								"Releasing child completion latch -  job {}",
								job.toDisplayName());
						childCompletionLatch.countDown();
					}
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
				Transaction.endAndBeginNew();
				AllocationQueue constraintQueue = queue;
				if ((queue.currentPhase == SubqueuePhase.Sequence
						|| queue.currentPhase == SubqueuePhase.Child)
						&& queue.job.provideParent().isPresent()) {
					/*
					 * use the parent constraints
					 */
					constraintQueue = queue.ensureParentQueue();
				}
				ExecutionConstraints executionConstraints = ExecutionConstraints
						.forQueue(constraintQueue);
				long maxAllocatable = queue.currentPhase == SubqueuePhase.Sequence
						? 1// essentially passing the allocation
							// slot to the next-in-sequence
						: executionConstraints
								.calculateMaxAllocatable(constraintQueue);
				// FIXME - mvcc.jobs.1a - allocate in batches (i.e.
				// 30...let drain to 10...again)
				if (maxAllocatable > 0) {
					if (queue.getUnallocatedJobs()
							.anyMatch(this::isAllocatable)) {
						ExecutorService executorService = executionConstraints
								.getExecutorServiceProvider()
								.getService(constraintQueue);
						List<Job> allocating = new ArrayList<>();
						Runnable allocateJobs = () -> {
							/*
							 * Double-checking
							 */
							Set<Job> invalidAllocated = new LinkedHashSet<>();
							queue.getUnallocatedJobs()
									.filter(this::isAllocatable)
									.limit(maxAllocatable)
									.forEach(allocating::add);
							allocating.forEach(j -> {
								if (j.getState() != JobState.PENDING) {
									logger.warn(
											"jobAllocator-invalid-state - not allocating job {}",
											j);
									invalidAllocated.add(j);
								} else {
									j.setState(JobState.ALLOCATED);
									j.setPerformer(ClientInstance.self());
									logger.debug("Allocated job {}", j);
									lastAllocated = System.currentTimeMillis();
								}
							});
							/*
							 * If this fails, we *want* it to throw an exception
							 * (for later retry)
							 */
							Transaction.commit();
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
																	launcherThreadState));
										}
									});
						};
						// FIXME - mvcc.jobs.1a - getting splurgey allocation?
						Object lock = JobRegistry.get().withJobMetadataLock(job,
								allocateJobs);
						if (lock == null) {
							logger.warn("Ran allocation job without lock? {}",
									job);
						}
						// if
						// (executionConstraints.isClusteredChildAllocation()) {
						// JobRegistry.get().withJobMetadataLock(job,
						// allocateJobs);
						// } else {
						// allocateJobs.run();
						// }
					}
				}
			}
			if (System.currentTimeMillis()
					- lastAllocated > TimeConstants.ONE_HOUR_MS
					&& jobContext != null
					&& jobContext.getJob().getPerformer() == ClientInstance
							.self()
					&& jobContext.getPerformer().canAbort(job.getTask())
					&& ResourceUtilities.is(Transactions.class,
							"cancelTimedoutTransactions")) {
				List<Job> incompleteChildren = job.provideChildren()
						.filter(j -> j.provideIsNotComplete()
								|| j.getState() == JobState.COMPLETED)
						.collect(Collectors.toList());
				logger.warn(
						"DEVEX::0 - Cancelling/aborting timed-out job - no allocations for one hour :: {} - incomplete children :: {}",
						job, incompleteChildren);
				Stream<Job> toAbort = incompleteChildren.isEmpty()
						? Stream.of(job)
						: incompleteChildren.stream();
				Stream<Job> toCancel = incompleteChildren.isEmpty()
						? Stream.empty()
						: Stream.of(job);
				toAbort.forEach(j -> {
					ResubmitPolicy policy = ResubmitPolicy.forJob(j);
					policy.visit(j);
					j.setState(JobState.ABORTED);
					j.setEndTime(new Date());
					j.setResultType(JobResultType.DID_NOT_COMPLETE);
				});
				toCancel.forEach(Job::cancel);
				Transaction.commit();
			}
			Transaction.ensureEnded();
		}

		@Override
		public void run() {
			thread = Thread.currentThread();
			while (!finished) {
				try {
					Event event = eventQueue.poll(1, TimeUnit.SECONDS);
					try {
						Transaction.begin();
						if (event == null) {
							/*
							 * TODO - probably remove this - it tends to only
							 * get hit while awaiting child jobs which are
							 * themselves awaiting sequence completion - which
							 * means it does nothing
							 */
							int debug = 3;
							/*
							 * FIXME - mvcc.jobs.1a - up the timeout n catch
							 * these suckers
							 */
							long incompleteCount = queue
									.getIncompleteAllocatedJobCountForCurrentPhaseThisVm();
							ExecutionConstraints constraints = ExecutionConstraints
									.forQueue(queue);
							ExecutorService executorService = constraints
									.getExecutorServiceProvider()
									.getService(queue);
							if (executorService instanceof ThreadPoolExecutor
									&& !constraints.isClusteredChildAllocation()
									&& queue.job.getCreator() == ClientInstance
											.self()
									&& queue.currentPhase == SubqueuePhase.Child) {
								ThreadPoolExecutor tpex = (ThreadPoolExecutor) executorService;
								if (tpex.getActiveCount() == 0
										&& incompleteCount > 0) {
									logger.warn(
											"Removing {} incomplete jobs as allocated/processing",
											incompleteCount);
									// queue.clearIncompleteAllocatedJobs();
									queue.cancelIncompleteAllocatedJobs();
								}
							}
							// missed event?
							queue.publish(EventType.WAKEUP);
						} else {
							DomainStore.waitUntilCurrentRequestsProcessed();
							processEvent(event);
							/*
							 * Only need to process the first
							 * RELATED_MODIFICATION
							 */
							if (event.type == EventType.RELATED_MODIFICATION) {
								Event peekEvent = null;
								while ((peekEvent = eventQueue.peek()) != null
										&& peekEvent.transactionId == event.transactionId
										&& peekEvent.type == EventType.RELATED_MODIFICATION) {
									eventQueue.poll();
								}
							}
						}
					} finally {
						Transaction.ensureEnded();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Thread.currentThread().setName("job-allocator::idle");
		}

		private boolean isPhaseComplete(Event event) {
			if (event.type == EventType.DELETED) {
				return true;
			}
			boolean selfPerformer = queue.job.getPerformer() == ClientInstance
					.self();
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
								.getPerformer() == ClientInstance.self();
			default:
				throw new UnsupportedOperationException();
			}
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
					&& queue.job.getPerformer() == ClientInstance.self()
					&& jobContext == null) {
				applyStatusMessage();
				Transaction.commit();
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
}
