package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * FIXME - mvcc.cascade - add to listjobs report
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
	private List<Job> activeJobs = new ArrayList<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	private JobScheduler scheduler;

	ProcessorThread thread;

	boolean finished;

	private BlockingQueue<Event> events = new LinkedBlockingQueue<>();

	public TowardsAMoreDesirableSituation(JobScheduler scheduler) {
		this.scheduler = scheduler;
	}

	private void addSchedulerEvent() {
		events.add(new Event(Type.SCHEDULER_EVENT));
	}

	private void futureToPending(Optional<Job> next) {
		Job job = next.get();
		job.setPerformer(ClientInstance.self());
		// FIXME - mvcc.cascade - this class is where
		// futureconsistency
		// deduplication should happen.
		// on (transform-based) job switch to
		// 'processing', for performer, snapshot
		// all future consistency jobs with same
		// signature
		// (taskserialized/taskclass)
		//
		// note resubmit is required for future
		// consistency aborts (unless task has other
		// logical consistency
		// ensurance mechanism - e.g. jade parsers)
		//
		job.setState(JobState.PENDING);
		JobDomain.get().getFutureConsistencyJobsEquivalentTo(job)
				.forEach(Job::delete);
		activeJobs.add(job);
		Transaction.commit();
		long currentPriorityCount = JobDomain.get()
				.getFutureConsistencyJobsCount(
						job.provideConsistencyPriority());
		logger.info(
				"TowardsAMoreDesirableSituation - consistency-to-pending - {} - {} - {},{} remaining",
				job, job.provideConsistencyPriority(), currentPriorityCount,
				JobDomain.get().getFutureConsistencyJobsCount());
	}

	void start() {
		thread = new ProcessorThread();
		thread.start();
		scheduler.eventOcurred.add((k, v) -> addSchedulerEvent());
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
			if (JobDomain.get().getFutureConsistencyJobs().findFirst()
					.isPresent()) {
				JobRegistry.get()
						.withJobMetadataLock(getClass().getSimpleName(), () -> {
							// allocate in bulk while holding lock
							while (canAllocate()) {
								Optional<Job> next = JobDomain.get()
										.getFutureConsistencyJobs().findFirst();
								if (next.isPresent()) {
									futureToPending(next);
								} else {
									break;
								}
							}
						});
			} else {
				break;
			}
		}
	}

	private boolean canAllocate() {
		return activeJobs.size() < JobRegistry.get().jobExecutors
				.getMaxConsistencyJobCount()
				&& JobRegistry.get().getActiveJobCount() < ResourceUtilities
						.getInteger(TowardsAMoreDesirableSituation.class,
								"maxVmActiveJobCount");
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
							// FIXME :: DEVEX.0
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
