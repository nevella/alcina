package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
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

	JobAllocator(AllocationQueue queue, ExecutorService allocatorService) {
		this.queue = queue;
		this.allocatorService = allocatorService;
		queue.events.add((k, e) -> enqueueEvent(e));
		lastStatus = new StatusMessage();
		if (queue.job.provideIsSelfStarter()) {
			start();
		}
	}

	public void awaitChildCompletion() {
		try {
			while (!childCompletionLatch.await(2, TimeUnit.SECONDS)) {
				int debug = 3;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void awaitSequenceCompletion() {
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
		if (allocationTask == null
				&& queueEvent.type == EventType.TO_PROCESSING) {
			start();
		}
	}

	public void fireDeletedEvent() {
		queue.publish(EventType.DELETED);
	}

	@Override
	public String toString() {
		return queue.toString();
	}

	void start() {
		allocationTask = new AllocationTask();
		allocatorService.execute(allocationTask);
	}

	void toAwaitingChildren() {
		if (queue.phase == SubqueuePhase.Self) {
			queue.publish(EventType.TO_AWAITING_CHILDREN);
		}
	}

	private class AllocationTask implements Runnable {
		boolean firstEvent = true;

		boolean finished = false;

		public void processEvent(Event event) {
			if (finished) {
				return;
			}
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
				childCompletionLatch.countDown();
				if (queue.phase == SubqueuePhase.Complete) {
					if (job.getState() == JobState.COMPLETED
							&& job.getPerformer() == ClientInstance.self()
							&& job.provideRelatedSequential().size() > 1) {
						job.setState(JobState.SEQUENCE_COMPLETE);
						Transaction.commit();
					}
				}
				sequenceCompletionLatch.countDown();
				finished = true;
				return;
			}
			logger.info("Allocation thread - job {} - event {}",
					job.toDisplayName(), event);
			if (isPhaseComplete(event)) {
				switch (queue.phase) {
				case Child:
				case Sequence:
				case Complete:
					logger.info("Releasing child completion latch -  job {}",
							job.toDisplayName());
					childCompletionLatch.countDown();
					break;
				}
				SubqueuePhase priorPhase = queue.phase;
				queue.incrementPhase();
				logger.info("Changed phase :: {} :: {} -> {}",
						job.toDisplayName(), priorPhase, queue.phase);
				// resubmit until event does not cause phase change
				// (or complete)
				enqueueEvent(event);
			} else {
				long maxAllocatable = ExecutionConstraints.forQueue(queue)
						.calculateMaxAllocatable();
				// FIXME - mvcc.jobs.1a - allocate in batches (i.e.
				// 30...let drain to 10...again)
				if (maxAllocatable > 0 && queue.getUnallocatedJobs()
						.anyMatch(this::isAllocatable)) {
					ExecutorService executorService = ExecutionConstraints
							.forQueue(queue).getExecutorServiceProvider()
							.getService(queue);
					List<Job> allocated = new ArrayList<>();
					JobRegistry.get().withJobMetadataLock(job, () -> {
						queue.getUnallocatedJobs().filter(this::isAllocatable)
								.limit(maxAllocatable).forEach(allocated::add);
						allocated.forEach(j -> {
							j.setState(JobState.ALLOCATED);
							j.setPerformer(ClientInstance.self());
							logger.info("Allocated job {}", j);
						});
						Transaction.commit();
					});
					allocated.forEach(j -> {
						LauncherThreadState launcherThreadState = new LauncherThreadState();
						executorService.submit(() -> JobRegistry.get()
								.performJob(j, false, launcherThreadState));
					});
					StatusMessage currentStatus = new StatusMessage();
					if (currentStatus.shouldPublish()) {
						currentStatus.publish();
						Transaction.commit();
					}
				}
			}
		}

		@Override
		public void run() {
			while (!finished) {
				try {
					Event event = eventQueue.poll(2, TimeUnit.SECONDS);
					try {
						if (event == null) {
							int debug = 3;
						} else {
							Transaction.begin();
							DomainStore.waitUntilCurrentRequestsProcessed();
							processEvent(event);
							Transaction firstEventTransaction = event.transaction;
							while ((event = eventQueue.peek()) != null
									&& event.transaction == firstEventTransaction) {
								eventQueue.poll();
								processEvent(event);
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

	private class StatusMessage {
		long publishTime;

		int percentComplete;

		long completedCount;

		long totalCount;

		SubqueuePhase phase;

		public StatusMessage() {
			phase = queue.phase;
			completedCount = queue.getCompletedJobCount();
			totalCount = queue.getTotalJobCount();
			percentComplete = (int) (((double) completedCount) / totalCount
					* 100.0);
			publishTime = System.currentTimeMillis();
		}

		public void publish() {
			lastStatus = this;
			String message = Ax.format("%s - %s% (%s/%s)", Ax.friendly(phase),
					percentComplete, completedCount, totalCount);
			queue.job.setStatusMessage(message);
		}

		public boolean shouldPublish() {
			if (phase == SubqueuePhase.Self
					|| phase == SubqueuePhase.Complete) {
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
