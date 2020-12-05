package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.AllocationQueue.Event;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.EventType;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob.SubqueuePhase;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobRegistry.LauncherThreadState;
import cc.alcina.framework.servlet.job.JobScheduler.ExecutionConstraints;

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

	JobAllocator(AllocationQueue queue, ExecutorService allocatorService) {
		this.queue = queue;
		this.allocatorService = allocatorService;
		queue.events.add((k, e) -> enqueueEvent(e));
		lastStatus = new StatusMessage();
		/*
		 * Allocator threads are started for top-level jobs iff:
		 * 
		 * -
		 */
		Job job = queue.job;
		boolean topLevelQueue = job.provideIsTopLevel()
				&& job.provideIsFirstInSequence();
		boolean visible = job.getCreator() == ClientInstance.self()
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

	void onFinished() {
		finished = true;
		new StatusMessage().publish();
		childCompletionLatch.countDown();
		sequenceCompletionLatch.countDown();
	}

	void toAwaitingChildren() {
		if (queue.phase == SubqueuePhase.Self) {
			queue.publish(EventType.TO_AWAITING_CHILDREN);
		}
	}

	private class AllocationTask implements Runnable {
		boolean firstEvent = true;

		public void processEvent(Event event) {
			if (finished) {
				return;
			}
			Transaction.ensureBegun();
			new StatusMessage().checkPublish();
			Job job = queue.job;
			if (firstEvent) {
				firstEvent = false;
				Thread.currentThread().setName(
						Ax.format("job-allocator::%s", queue.toDisplayName()));
				logger.info("Allocation thread started -  job {}",
						job.toDisplayName());
			}
			if (queue.phase == SubqueuePhase.Complete
					|| job.resolveState() == JobState.CANCELLED
					|| job.resolveState() == JobState.ABORTED) {
				logger.info("Allocation thread ended -  job {}",
						job.toDisplayName());
				if (queue.phase == SubqueuePhase.Complete) {
					if (job.getState() == JobState.COMPLETED
							&& job.getPerformer() == ClientInstance.self()
							&& job.provideRelatedSequential().size() > 1) {
						job.setState(JobState.SEQUENCE_COMPLETE);
						Transaction.commit();
					}
				}
				onFinished();
				return;
			}
			logger.info("Allocation thread - job {} - event {}",
					job.toDisplayName(), event);
			if (isPhaseComplete(event)) {
				if (queue.phase == SubqueuePhase.Child) {
					/*
					 * only countdown the child latch if we'll be waiting on
					 * sequence
					 */
					if (queue.job.provideNextInSequence().isPresent()) {
						logger.info(
								"Releasing child completion latch -  job {}",
								job.toDisplayName());
						childCompletionLatch.countDown();
					}
				}
				SubqueuePhase priorPhase = queue.phase;
				queue.incrementPhase();
				logger.info("Changed phase :: {} :: {} -> {}",
						job.toDisplayName(), priorPhase, queue.phase);
				// resubmit until event does not cause phase change
				// (or complete)
				new StatusMessage().publish();
				enqueueEvent(event);
			} else {
				Transaction.endAndBeginNew();
				ExecutionConstraints executionConstraints = ExecutionConstraints
						.forQueue(queue);
				long maxAllocatable = executionConstraints
						.calculateMaxAllocatable();
				// FIXME - mvcc.jobs.1a - allocate in batches (i.e.
				// 30...let drain to 10...again)
				if (maxAllocatable > 0) {
					if (queue.getUnallocatedJobs()
							.anyMatch(this::isAllocatable)) {
						ExecutorService executorService = executionConstraints
								.getExecutorServiceProvider().getService(queue);
						List<Job> allocated = new ArrayList<>();
						Runnable allocateJobs = () -> {
							// FIXME - mvcc.jobs.1a - check this doesn't
							// traverse more than it needs to
							queue.getUnallocatedJobs()
									.filter(this::isAllocatable)
									.limit(maxAllocatable)
									.forEach(allocated::add);
							allocated.forEach(j -> {
								j.setState(JobState.ALLOCATED);
								j.setPerformer(ClientInstance.self());
								logger.info("Allocated job {}", j);
							});
							Transaction.commit();
						};
						if (executionConstraints.isClusteredChildAllocation()) {
							JobRegistry.get().withJobMetadataLock(job,
									allocateJobs);
						} else {
							allocateJobs.run();
						}
						allocated.forEach(j -> {
							LauncherThreadState launcherThreadState = new LauncherThreadState();
							executorService.submit(() -> JobRegistry.get()
									.performJob(j, false, launcherThreadState));
						});
					}
				}
			}
			Transaction.ensureEnded();
		}

		@Override
		public void run() {
			while (!finished) {
				try {
					Event event = eventQueue.poll(1, TimeUnit.SECONDS);
					try {
						Transaction.begin();
						if (event == null) {
							int debug = 3;
							/*
							 * FIXME - mvcc.jobs.1a - up the timeout n catch
							 * these suckers
							 */
							long incompleteCount = queue
									.getIncompleteAllocatedJobCountForCurrentPhase();
							ExecutionConstraints constraints = ExecutionConstraints
									.forQueue(queue);
							ExecutorService executorService = constraints
									.getExecutorServiceProvider()
									.getService(queue);
							if (executorService instanceof ThreadPoolExecutor
									&& !constraints.isClusteredChildAllocation()
									&& queue.job.getCreator() == ClientInstance
											.self()
									&& queue.phase == SubqueuePhase.Child) {
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
										&& peekEvent.transaction == event.transaction
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
			boolean selfPerformer = queue.job.getCreator() == ClientInstance
					.self();
			switch (queue.phase) {
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
			default:
				throw new UnsupportedOperationException();
			}
		}

		boolean isAllocatable(Job job) {
			switch (queue.phase) {
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
				Preconditions.checkArgument(
						job.providePreviousOrSelfInSequence() != job);
				return job.providePreviousOrSelfInSequence().provideIsComplete()
						&& job.providePreviousOrSelfInSequence()
								.getCreator() == ClientInstance.self();
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
			phase = queue.phase;
			completedCount = queue.getCompletedJobCount();
			if (!queue.job.provideNextInSequence().isPresent()
					&& queue.job.getState() == JobState.PROCESSING
					&& finished) {
				completedCount++;
			}
			totalCount = queue.getTotalJobCount();
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
			if (queue.phase == SubqueuePhase.Sequence
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
