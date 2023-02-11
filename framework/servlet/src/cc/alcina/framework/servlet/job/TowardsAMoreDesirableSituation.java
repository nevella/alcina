package cc.alcina.framework.servlet.job;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 *
 * <h2>Model consistency</h2>
 * <p>
 * The following may not be fully implemented
 * </p>
 * <ul>
 * <li>Future consistency is a property of the domain - if a change in A makes B
 * inconsistent, generate a job which will make B consistent in the same
 * domain/db transaction as the change to A.
 * <li>Consistencies can have different priorities (based on the job.consistency
 * property order) (TODO)
 * <li>Consistencies are generally better handled by resubmit processors (TODO:
 * detail)
 * <li>
 * </ul>
 */
class TowardsAMoreDesirableSituation {
	private List<Job> activeJobs = Collections
			.synchronizedList(new ArrayList<>());

	Logger logger = LoggerFactory.getLogger(getClass());

	private JobScheduler scheduler;

	ProcessorThread thread;

	boolean finished;

	private BlockingQueue<Event> events = new LinkedBlockingQueue<>();

	public TowardsAMoreDesirableSituation(JobScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public Stream<? extends Job> getActiveJobs() {
		// thread-safe copy
		synchronized (activeJobs) {
			return activeJobs.stream().collect(Collectors.toList()).stream();
		}
	}

	private void addSchedulerEvent() {
		events.add(new Event(Type.SCHEDULER_EVENT));
	}

	private boolean canAllocate() {
		return activeJobs.size() < JobRegistry.get().jobExecutors
				.getMaxConsistencyJobCount()
				&& JobRegistry.get().getActiveJobCount() < ResourceUtilities
						.getInteger(TowardsAMoreDesirableSituation.class,
								"maxVmActiveJobCount");
	}

	private void futureToPending(Optional<Job> next) {
		Timestamp entryRequiredTimestamp = JobRegistry.get()
				.getJobMetadataLockTimestamp(getClass().getSimpleName());
		DomainTransformCommitPosition entryPosition = DomainStore.stores()
				.writableStore().getPersistenceEvents().getQueue()
				.getTransformCommitPosition();
		// note resubmit is required for future
		// consistency aborts (unless task has other
		// logical consistency
		// ensurance mechanism - e.g. jade parsers)
		//
		Job job = next.get();
		if (job.getPerformer() != null) {
			logger.info(
					"TowardsAMoreDesirableSituation - fatal - non-null performer - {}",
					job);
		}
		job.setPerformer(ClientInstance.self());
		job.setState(JobState.PENDING);
		JobDomain.get().getFutureConsistencyJobsEquivalentTo(job)
				.forEach(Job::delete);
		activeJobs.add(job);
		Transaction.commit();
		DomainTransformCommitPosition exitPosition = DomainStore.stores()
				.writableStore().getPersistenceEvents().getQueue()
				.getTransformCommitPosition();
		long currentPriorityCount = JobDomain.get()
				.getFutureConsistencyJobsCount(
						job.provideConsistencyPriority());
		logger.info(
				"TowardsAMoreDesirableSituation - consistency-to-pending - {} - {} - {},{} remaining - entry: {} required {} - exit: {} required {}",
				job, job.provideConsistencyPriority(), currentPriorityCount,
				JobDomain.get().getFutureConsistencyJobsCount(), entryPosition,
				exitPosition);
	}

	void start() {
		thread = new ProcessorThread();
		thread.start();
		scheduler.eventOcurred.add(v -> addSchedulerEvent());
	}

	void stopService() {
		events.add(new Event(Type.SHUTDOWN));
	}

	void tend() {
		if (!ResourceUtilities.is("enabled")) {
			return;
		}
		activeJobs.removeIf(
				j -> j.domain().wasRemoved() || j.provideIsSequenceComplete());
		boolean delta = false;
		while (canAllocate()) {
			AtomicInteger skipCount = new AtomicInteger();
			if (JobDomain.get().getFutureConsistencyJobs().findFirst()
					.isPresent()) {
				JobRegistry.get()
						.withJobMetadataLock(getClass().getSimpleName(), () -> {
							Transaction.endAndBeginNew();
							// allocate in bulk while holding lock
							while (canAllocate()) {
								Stream<Job> stream = JobDomain.get()
										.getFutureConsistencyJobs();
								ConsistencyJobFilter filter = ConsistencyJobFilter
										.get();
								Optional<Job> next = stream.filter(filter)
										.findFirst();
								if (next.isPresent()) {
									if (skipCount.get() > 0) {
										logger.info(
												"Allocating (future -> pending) after {} skips",
												skipCount);
									}
									futureToPending(next);
									break;
								} else {
									skipCount.incrementAndGet();
								}
							}
						});
				if (skipCount.get() > 0) {
					try {
						// let other cluster instances try
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else {
				break;
			}
		}
	}

	public class ProcessorThread extends Thread {
		@Override
		public void run() {
			setName("Towards-a-more-queue-"
					+ EntityLayerUtils.getLocalHostName());
			while (!finished) {
				try {
					Event event = events.take();
					if (event.type == Type.SHUTDOWN) {
						finished = true;
					} else {
						Transaction.ensureBegun();
						tend();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					try {
						Transaction.ensureEnded();
					} catch (Exception e) {
						if (TransformManager.get() == null) {
							// shutting down
						} else {
							logger.warn("DEVEX::0 - unknown", e);
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	static class Event {
		Type type;

		public Event(Type type) {
			this.type = type;
		}
	}

	enum Type {
		SCHEDULER_EVENT, SHUTDOWN;
	}
}
