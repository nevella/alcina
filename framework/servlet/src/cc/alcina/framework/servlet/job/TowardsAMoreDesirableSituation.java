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
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/*
 * FIXME - mvcc.cascade - add to listjobs report
 */
class TowardsAMoreDesirableSituation {
	private List<Job> activeJobs = new ArrayList<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	private JobScheduler scheduler;

	public TowardsAMoreDesirableSituation(JobScheduler scheduler) {
		this.scheduler = scheduler;
	}

	void tend() {
		if (!ResourceUtilities.is("enabled")) {
			return;
		}
		activeJobs.removeIf(Job::provideIsSequenceComplete);
		boolean delta = false;
		while (activeJobs.size() < JobRegistry.get().jobExecutors
				.getMaxConsistencyJobCount()
				&& JobRegistry.get().getActiveJobCount() < ResourceUtilities
						.getInteger(TowardsAMoreDesirableSituation.class,
								"maxVmActiveJobCount")) {
			if (JobDomain.get().getFutureConsistencyJobs().findFirst()
					.isPresent()) {
				JobRegistry.get()
						.withJobMetadataLock(getClass().getSimpleName(), () -> {
							Optional<Job> next = JobDomain.get()
									.getFutureConsistencyJobs().findFirst();
							if (next.isPresent()) {
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
								activeJobs.add(job);
								Transaction.commit();
								logger.info(
										"TowardsAMoreDesirableSituation - consistency-to-pending - {} - {} remaining",
										job,
										JobDomain.get()
												.getFutureConsistencyJobs()
												.count());
							}
						});
			} else {
				break;
			}
		}
	}

	static class Event {
		public Event(Type type) {
			this.type = type;
		}

		Type type;
	}

	ProcessorThread thread;

	boolean finished;

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

	enum Type {
		SCHEDULER_EVENT, SHUTDOWN;
	}

	private BlockingQueue<Event> events = new LinkedBlockingQueue<>();

	void start() {
		thread = new ProcessorThread();
		thread.start();
		scheduler.eventOcurred
				.add((k, v) -> addSchedulerEvent());
	}

	private void addSchedulerEvent() {
		events.add(new Event(Type.SCHEDULER_EVENT));
	}

	void stopService() {
		events.add(new Event(Type.SHUTDOWN));
	}
}
