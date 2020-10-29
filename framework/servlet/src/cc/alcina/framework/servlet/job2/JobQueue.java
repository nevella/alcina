package cc.alcina.framework.servlet.job2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.cache.descriptor.DomainDescriptorJob;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.GraphProjection;

/*
 * Threading - all access is from the launching job thread. Access to the field metadataChanged 
 */
public class JobQueue {
	ExecutorService executorService;

	Job initialJob;

	private int maxConcurrentJobs;

	private String name;

	private List<Job> active = new LinkedList<>();

	private List<Job> pending = new LinkedList<>();

	private boolean cancelled = false;

	private boolean metadataChanged = false;

	private boolean finished;

	private List<CountDownLatch> finishedLatches = new ArrayList<>();

	public JobQueue(Job initialJob, ExecutorService executorService,
			int maxConcurrentJobs, boolean clustered) {
		this.name = Ax.format("%s - %s/%s", initialJob.getKey(),
				initialJob.getLocalId(),
				EntityLayerObjects.get().getServerAsClientInstance().getId());
		this.initialJob = initialJob;
		this.executorService = executorService;
		this.maxConcurrentJobs = maxConcurrentJobs;
		initialJob.setQueue(name);
	}

	public String getName() {
		return name;
	}

	public void onMetadataChanged() {
		synchronized (this) {
			metadataChanged = true;
			notify();
		}
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToString(this);
	}

	private void acquireAllocationLock() {
		Preconditions.checkState(!initialJob.isClustered());
	}

	private void allocateJobs() {
		pending.removeIf(Job::provideIsComplete);
		active.removeIf(Job::provideIsComplete);
		long limit = maxConcurrentJobs - (active.size() + pending.size())
				+ calculateDesiredPendingSize();
		if (limit > 0) {
			DomainDescriptorJob.get().getUnallocatedJobsForQueue(name)
					.limit(limit).forEach(job -> {
						job.setPerformer(EntityLayerObjects.get()
								.getServerAsClientInstance());
						pending.add(job);
					});
			Transaction.commit();
		}
	}

	private int calculateDesiredPendingSize() {
		return 0;
	}

	private void releaseAllocationLock() {
	}

	void allocateUntilEmpty() {
		while (!cancelled) {
			try {
				acquireAllocationLock();
				allocateJobs();
				boolean submitted = false;
				if (active.size() < maxConcurrentJobs) {
					if (pending.size() > 0) {
						Job toSubmit = pending.remove(0);
						active.add(toSubmit);
						executorService.submit(
								() -> JobRegistry.get().performJob(toSubmit));
					}
				}
				if (active.isEmpty() && pending.isEmpty()) {
					JobRegistry.get().onJobQueueTerminated(this);
					synchronized (this) {
						finished = true;
						finishedLatches.forEach(CountDownLatch::countDown);
					}
					return;
				}
				synchronized (this) {
					if (!metadataChanged) {
						wait(10000);
					}
					Transaction.endAndBeginNew();
					metadataChanged = false;
				}
			} catch (Exception e) {
				JobContext.current().onJobException(e);
				throw WrappedRuntimeException.wrapIfNotRuntime(e);
			} finally {
				releaseAllocationLock();
			}
		}
	}

	void awaitEmpty() {
		synchronized (this) {
			if (finished) {
				return;
			}
			finishedLatches.add(new CountDownLatch(1));
		}
	}

	void cancel() {
		cancelled = true;
		active.forEach(Job::cancel);
		onMetadataChanged();
	}

	public static interface AllocationLocker {
		Object lock();

		void unlock(Object lock);
	}
}
