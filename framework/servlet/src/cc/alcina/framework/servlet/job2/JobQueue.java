package cc.alcina.framework.servlet.job2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.servlet.job2.JobRegistry.QueueStat;
import cc.alcina.framework.servlet.job2.JobScheduler.Schedule;

/*
 * Threading - all access is from the launching job thread, except access to the fields metadataChanged/finished/finishedLatches (synchronized)  
 */
public class JobQueue {
	ExecutorService executorService;

	Job initialJob;

	private int maxConcurrentJobs;

	private String name;

	private List<Job> active = new LinkedList<>();

	private List<Job> pending = new LinkedList<>();

	private boolean cancelled = false;

	private LinkedList<Long> metadataChanged = new LinkedList<>();

	private AtomicLong metadataChangedCounter = new AtomicLong();

	private boolean finished;

	private List<CountDownLatch> finishedLatches = new ArrayList<>();

	private JobRegistry jobRegistry = JobRegistry.get();

	private Schedule schedule;

	volatile boolean allocating;

	Logger logger = LoggerFactory.getLogger(getClass());

	public JobQueue(Job initialJob, ExecutorService executorService,
			int maxConcurrentJobs, boolean clustered) {
		this.name = Ax.format("%s - %s/%s", initialJob.getTaskClassName(),
				initialJob.getLocalId(),
				EntityLayerObjects.get().getServerAsClientInstance().getId());
		this.initialJob = initialJob;
		this.executorService = executorService;
		this.maxConcurrentJobs = maxConcurrentJobs;
		initialJob.setQueue(name);
	}

	public JobQueue(Schedule schedule) {
		this.schedule = schedule;
		this.name = schedule.getQueueName();
		this.executorService = schedule.getExcutorServiceProvider()
				.getService();
		this.maxConcurrentJobs = schedule.getQueueMaxConcurrentJobs();
	}

	public void ensureAllocating() {
		synchronized (this) {
			if (!allocating) {
				finished = false;
				allocating = true;
				AlcinaChildRunnable.runInTransactionNewThread(name,
						this::loopAllocations);
			}
		}
	}

	public String getName() {
		return name;
	}

	public void onMetadataChanged() {
		synchronized (metadataChanged) {
			ensureAllocating();
			long event = metadataChangedCounter.get();
			logger.debug("publish metadatachanged: {} {}", name, event);
			metadataChanged.add(event);
			metadataChanged.notifyAll();
		}
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToString(this);
	}

	// TODO - document
	private void allocateJobs() {
		pending.removeIf(Job::provideIsComplete);
		active.removeIf(Job::provideIsComplete);
		boolean scheduled = schedule != null;
		/*
		 * jobs either time-wise scheduled (limited)
		 * maxConcurrent/executor-limited. If time-wise scheduled, only the
		 * cluster leader should perform
		 */
		if (scheduled && schedule.isTimewiseLimited()) {
			if (!jobRegistry.jobExecutors.isCurrentScheduledJobExecutor()) {
				return;
			}
			int activeJobs = DomainDescriptorJob.get().getActiveJobCount(name);
			Optional<? extends Job> unallocated = DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(name, true)
					.filter(Job::provideIsAllocatable)
					.sorted(new Job.RunAtComparator()).findFirst();
			if (unallocated.isPresent()) {
				jobRegistry.withJobMetadataLock(name, isClustered(), () -> {
					Job job = unallocated.get();
					job.setPerformer(EntityLayerObjects.get()
							.getServerAsClientInstance());
					if (schedule.getQueueMaxConcurrentJobs() <= activeJobs) {
						job.setState(JobState.SKIPPED);
					} else {
						pending.add(job);
					}
					Transaction.commit();
				});
			}
		} else {
			long limit = maxConcurrentJobs - (active.size() + pending.size())
					+ calculateDesiredPendingSize();
			if (limit > 0) {
				jobRegistry.withJobMetadataLock(name, isClustered(), () -> {
					DomainDescriptorJob.get()
							.getUnallocatedJobsForQueue(name, true).limit(limit)
							.forEach(job -> {
								job.setPerformer(EntityLayerObjects.get()
										.getServerAsClientInstance());
								pending.add(job);
							});
					Transaction.commit();
				});
			}
		}
	}

	// TODO - for large distributed jobs, get an adjustable chunk of 'pending'
	// to reduce contention/db writes
	private int calculateDesiredPendingSize() {
		return 0;
	}

	private int getMetadataChangedQueueLength() {
		synchronized (metadataChanged) {
			return metadataChanged.size();
		}
	}

	private void onQueueFinished() {
		synchronized (this) {
			if (finished) {
				return;
			}
			jobRegistry.onJobQueueTerminated(this);
			finished = true;
			finishedLatches.forEach(CountDownLatch::countDown);
			allocating = false;
		}
	}

	protected boolean isClustered() {
		return schedule != null ? schedule.isClustered()
				: initialJob.isClustered();
	}

	QueueStat asQueueStat() {
		QueueStat stat = new QueueStat();
		stat.name = name;
		stat.active = active.size();
		stat.pending = pending.size();
		stat.total = (int) DomainDescriptorJob.get().getJobsForQueue(name)
				.count();
		return stat;
	}

	void awaitEmpty() {
		CountDownLatch latch = null;
		synchronized (this) {
			if (finished) {
				return;
			}
			latch = new CountDownLatch(1);
			finishedLatches.add(latch);
		}
		try {
			if (!allocating) {
				allocating = true;
				loopAllocations();
			}
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	void cancel() {
		cancelled = true;
		active.forEach(Job::cancel);
		onMetadataChanged();
		onQueueFinished();
	}

	void loopAllocations() {
		while (!cancelled) {
			try {
				allocateJobs();
				boolean submitted = false;
				if (active.size() < maxConcurrentJobs) {
					if (pending.size() > 0) {
						Job toSubmit = pending.remove(0);
						active.add(toSubmit);
						executorService
								.submit(() -> jobRegistry.performJob(toSubmit));
					}
				}
				logger.debug(
						"allocation status metadatachanged: {} - active {} - pending {}",
						name, active, pending.size());
				//
				if (active.isEmpty() && pending.isEmpty() && (schedule == null
						|| schedule.getQueueMaxConcurrentJobs() != 1)) {
					onQueueFinished();
					return;
				}
				logger.debug("await metadatachanged");
				boolean hadEvent = false;
				Long id = null;
				if (getMetadataChangedQueueLength() == 0) {
					synchronized (metadataChanged) {
						metadataChanged.wait(10000);
					}
				}
				if (getMetadataChangedQueueLength() > 0) {
					synchronized (metadataChanged) {
						id = metadataChanged.removeFirst();
						logger.debug("Removed event from toFire: {}", id);
					}
				} else {
					// FIXME - the timeout *shouldn't* be needed -
					// but...kafka timeouts...gcs...
					if (name.matches(
							ResourceUtilities.get("logMetadataTimeouts"))) {
						logger.info("Timed out waiting for metadata change: {}",
								name);
					}
				}
				DomainStore.waitUntilCurrentRequestsProcessed();
			} catch (Exception e) {
				if (JobContext.get() != null) {
					JobContext.get().onJobException(e);
				}
				e.printStackTrace();
				throw WrappedRuntimeException.wrapIfNotRuntime(e);
			} finally {
			}
		}
	}

	public static interface AllocationLocker {
		Object lock();

		void unlock(Object lock);
	}
}
