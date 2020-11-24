package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import cc.alcina.framework.common.client.util.InnerAccess;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.servlet.job.JobRegistry.LauncherThreadState;
import cc.alcina.framework.servlet.job.JobRegistry.QueueStat;
import cc.alcina.framework.servlet.job.JobScheduler.Schedule;

/*
 * Threading - all access is from the launching job thread, except access to the fields metadataChanged/finished/finishedLatches (synchronized)  
 */
public class JobQueue {
	private static Map<Job, Allocation> submittedJobs = new LinkedHashMap<>();

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

	boolean queueJobPersistence = false;

	boolean shutdownExecutorOnExit;

	public JobQueue(Job initialJob, ExecutorService executorService,
			int maxConcurrentJobs, boolean clustered) {
		this.name = Ax.format("%s - %s/%s", initialJob.getTaskClassName(),
				initialJob.getLocalId(),
				EntityLayerObjects.get().getServerAsClientInstance().getId());
		this.initialJob = initialJob;
		this.executorService = executorService;
		this.maxConcurrentJobs = maxConcurrentJobs;
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
			boolean hasUnallocated = DomainDescriptorJob.get()
					.getUnallocatedJobsForQueue(name, true)
					.anyMatch(Job::provideIsAllocatable);
			if (hasUnallocated
					&& schedule.getQueueMaxConcurrentJobs() > activeJobs) {
				jobRegistry.withJobMetadataLock(name, isClustered(), () -> {
					/*
					 * essentially double-checked locking (rather than getting
					 * unallocated outside the lock)
					 */
					Optional<? extends Job> unallocated = DomainDescriptorJob
							.get().getUnallocatedJobsForQueue(name, true)
							.filter(Job::provideIsAllocatable)
							.sorted(new Job.RunAtComparator()).findFirst();
					Job job = unallocated.get();
					job.setPerformer(EntityLayerObjects.get()
							.getServerAsClientInstance());
					if (schedule.getQueueMaxConcurrentJobs() <= activeJobs) {
						// FIXME - mvcc.jobs - probably remove 'skipped'. The
						// 'logic' around 'skipped' is problematic - it was
						// intended to handle
						// spammy scheduling of timewise-constrained tasks.
						// But...the scheduler only has max-1 future per-class
						// task, so it's not needed (and wrong)
						// job.setState(JobState.SKIPPED);
					} else {
						job.setState(JobState.ALLOCATED);
						pending.add(job);
					}
					Transaction.commit();
				});
			}
		} else {
			// FIXME - mvcc.jobs - maxConcurrentJobs is a cluster property, not
			// a per-vm
			//
			// At the moment, it's just 'largeish' (100) for bulk jobs so that
			// all vms do some bursty allocation
			//
			// but it should really tail off towards the end of the bulk job
			// (depdendent on #vms)
			InnerAccess<Long> maxAllocated = InnerAccess
					.of((long) (maxConcurrentJobs
							- (active.size() + pending.size())));
			if (schedule != null) {
				long scheduleMaxAllocated = schedule.calculateMaxAllocated(this,
						active.size() + pending.size(),
						DomainDescriptorJob.get()
								.getJobCountForActiveQueue(name),
						DomainDescriptorJob.get()
								.getUnallocatedJobCountForQueue(name));
				if (scheduleMaxAllocated != -1) {
					maxAllocated.set(scheduleMaxAllocated);
				}
			}
			if (maxAllocated.get() > 0) {
				jobRegistry.withJobMetadataLock(name, isClustered(), () -> {
					DomainDescriptorJob.get()
							.getUnallocatedJobsForQueue(name, true)
							.filter(Job::provideIsAllocatable)
							.limit(maxAllocated.get()).forEach(job -> {
								job.setPerformer(EntityLayerObjects.get()
										.getServerAsClientInstance());
								job.setState(JobState.ALLOCATED);
								pending.add(job);
								logger.info("Queue: {}@{} - allocated: {}",
										name, Integer.toHexString(hashCode()),
										job.getId());
							});
					Transaction.commit();
				});
				// if >10, we're in the middle of a bulk run, so queue
				// non-allocation persistence (watcher updates will be slower
				// but *much* less load on the transform queues
				// queueJobPersistence = maxAllocated.get() > 10;
			}
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
			if (shutdownExecutorOnExit) {
				executorService.shutdown();
			}
		}
	}

	protected boolean isClustered() {
		return schedule != null ? schedule.isClustered()
				: initialJob.isClustered();
	}

	QueueStat asQueueStat() {
		QueueStat stat = new QueueStat();
		stat.name = name;
		stat.active = DomainDescriptorJob.get().getJobCountForActiveQueue(name,
				JobState.PROCESSING);
		stat.pending = DomainDescriptorJob.get().getJobCountForActiveQueue(name,
				JobState.PENDING)
				+ DomainDescriptorJob.get().getJobCountForActiveQueue(name,
						JobState.ALLOCATED);
		stat.total = DomainDescriptorJob.get().getJobCountForActiveQueue(name);
		stat.completed = DomainDescriptorJob.get()
				.getCompletedJobCountForActiveQueue(name);
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
				Transaction.ensureBegun();
				loopAllocations();
			}
			Transaction.ensureEnded();
			latch.await();
			Transaction.begin();
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

	Schedule getSchedule() {
		return this.schedule;
	}

	void loopAllocations() {
		while (!cancelled) {
			try {
				allocateJobs();
				if (active.size() < maxConcurrentJobs) {
					if (pending.size() > 0) {
						Job toSubmit = pending.remove(0);
						active.add(toSubmit);
						Allocation existing = null;
						Allocation allocation = new Allocation();
						logger.info("Submitting job: {}", toSubmit);
						synchronized (submittedJobs) {
							existing = submittedJobs.put(toSubmit, allocation);
						}
						if (existing != null) {
							// FIXME - mvcc.jobs - *definitely* shouldn't
							// occur,
							// but there is something funny going on...
							// probably fixed (wasn't respecting performer) -
							// can remove after production testing
							logger.warn(
									"Double allocation:\n============\nFirst:\n{}\n============Second:\n{}\n",
									existing, allocation);
						} else {
							LauncherThreadState launcherThreadState = new LauncherThreadState();
							executorService.submit(() -> jobRegistry.performJob(
									toSubmit, queueJobPersistence,
									launcherThreadState));
						}
					}
				}
				logger.debug(
						"allocation status metadatachanged: {} - active {} - pending {}",
						name, active, pending.size());
				//
				if (active.isEmpty() && pending.isEmpty()) {
					onQueueFinished();
					return;
				}
				logger.debug("await metadatachanged");
				boolean hadEvent = false;
				Long id = null;
				synchronized (metadataChanged) {
					if (metadataChanged.isEmpty()) {
						metadataChanged.wait(10000);
					}
					if (metadataChanged.size() > 0) {
						id = metadataChanged.removeFirst();
						logger.debug("Removed event from toFire: {}", id);
					} else {
						// FIXME - the timeout *shouldn't* be needed -
						// but...kafka timeouts...gcs...
						if (name.matches(
								ResourceUtilities.get("logMetadataTimeouts"))) {
							logger.info(
									"Timed out waiting for metadata change: {}",
									name);
						}
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

	@SuppressWarnings("unused")
	private class Allocation {
		private String trace;

		private long threadId;

		private String threadName;

		private int pendingHash;

		private Date date;

		public Allocation() {
			Thread currentThread = Thread.currentThread();
			this.threadName = currentThread.getName();
			this.threadId = currentThread.getId();
			this.trace = SEUtilities.getFullStacktrace(currentThread);
			this.pendingHash = pending.hashCode();
			this.date = new Date();
		}

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToString(this);
		}
	}
}
