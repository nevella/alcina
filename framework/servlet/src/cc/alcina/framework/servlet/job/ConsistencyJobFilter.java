package cc.alcina.framework.servlet.job;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * Override this to allow app-specific filtering (for instance, ignoring jobs
 * which would hit a lock because of a currently-performing job)
 */
@Registration.Singleton
public class ConsistencyJobFilter implements Predicate<Job> {
	public static ConsistencyJobFilter get() {
		return Registry.impl(ConsistencyJobFilter.class);
	}

	private List<Job> locallyEnqueuedConsistencyJobs;

	AtomicInteger skipCount = new AtomicInteger();

	public List<Job> getLocallyEnqueuedConsistencyJobs() {
		return locallyEnqueuedConsistencyJobs;
	}

	public AtomicInteger getSkipCount() {
		return this.skipCount;
	}

	public void setLocallyEnqueuedConsistencyJobs(
			List<Job> locallyEnqueuedConsistencyJobs) {
		this.locallyEnqueuedConsistencyJobs = locallyEnqueuedConsistencyJobs;
	}

	@Override
	public final boolean test(Job job) {
		boolean result = test0(job);
		if (!result) {
			skipCount.incrementAndGet();
		}
		return result;
	}

	protected boolean test0(Job job) {
		return true;
	}
}