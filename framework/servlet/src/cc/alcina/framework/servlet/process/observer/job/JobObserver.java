package cc.alcina.framework.servlet.process.observer.job;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;

/**
 * <p>
 * This class gathers detailed logs of the events that occur during the
 * lifecycle of a Job.
 * <p>
 * It has considerable overlap with MvccObserver - if adding any more Observer
 * types, abstract the base.
 * <p>
 * Histories are evicted 1 minute after job completion (the eviction 'pump' is
 * driven by job events, so the most recent job may be retained for longer). Set
 * <code>JobObserver.TimedOutEventObserver.record</code> to persist timedout
 * histories
 */
@Registration.Singleton
public class JobObserver {
	static JobObserver get() {
		return Registry.impl(JobObserver.class);
	}

	public static JobHistory getHistory(EntityLocator locator) {
		return get().getHistory0(locator);
	}

	public static void observe(ObservableJobFilter filter) {
		get().observe0(filter);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	/*
	 * Since EntityLocator comparison is complex around promoted objects, use
	 * the localId only
	 */
	ConcurrentHashMap<Long, JobHistory> histories = new ConcurrentHashMap<>();

	List<ObservableJobFilter> filters = new CopyOnWriteArrayList<>();

	List<EvictionRecord> evictionRecords = new CopyOnWriteArrayList<>();

	JobObserver() {
		JobDomain.get().topicLogObservations
				.add(job -> getHistory0(job.toLocator()).sequence()
						.withIncludeMvccObservables(true).exportLocal());
	}

	void checkEviction() {
		while (evictionRecords.size() > 0) {
			EvictionRecord record = evictionRecords.get(0);
			if (record.evict()) {
				evictionRecords.remove(0);
			} else {
				break;
			}
		}
	}

	void conditionallyRecord(JobObservable event) {
		JobHistory history = getHistoryForEvent(event);
		if (history != null) {
			history.add(event);
		}
		checkEviction();
	}

	JobHistory getHistoryForEvent(JobObservable event) {
		return histories.get(getJobLocalId(event));
	}

	private long getJobLocalId(JobObservable event) {
		return event.job.getLocalId();
	}

	JobHistory getHistory0(EntityLocator locator) {
		return histories.get(locator.localId);
	}

	boolean isObserving(JobObservable event) {
		long localId = getJobLocalId(event);
		return localId != 0 && histories.containsKey(localId);
	}

	void observe0(ObservableJobFilter filter) {
		filters.add(filter);
		if (filters.size() == 1) {
			logger.info("registering ObservableJobFilter");
			registerObservers();
		}
	}

	void registerObservers() {
		new CreatedObserver().bind();
		new EndedObserver().bind();
		new ToProcessingObserver().bind();
		new AllocationEventObserver().bind();
		new AllocatorCreatedObserver().bind();
		new AllocationThreadStartedObserver().bind();
		new TimedOutEventObserver().bind();
	}

	class TimedOutEventObserver
			implements ProcessObserver<JobObservable.TimedOut> {
		@Override
		public void topicPublished(JobObservable.TimedOut event) {
			logger.info("TimedOutEventObserver - observed {}", event);
			if (Configuration.is("record")) {
				JobHistory history = getHistoryForEvent(event);
				if (history != null) {
					history.sequence().withIncludeMvccObservables(true)
							.exportLocal();
				}
			}
		}
	}

	class AllocationEventObserver
			implements ProcessObserver<JobObservable.AllocationEvent> {
		@Override
		public void topicPublished(JobObservable.AllocationEvent event) {
			conditionallyRecord(event);
		}
	}

	class CreatedObserver implements ProcessObserver<JobObservable.Created> {
		@Override
		public void topicPublished(JobObservable.Created event) {
			if (filters.stream().anyMatch(f -> f.isBeginObservation(event))) {
				if (!isObserving(event)) {
					logger.debug("Begin observing - job localid {}",
							event.job.getLocalId());
					histories.put(event.job.getLocalId(),
							new JobHistory(event.job));
				}
			}
			conditionallyRecord(event);
		}
	}

	class EndedObserver implements ProcessObserver<JobObservable.Ended> {
		@Override
		public void topicPublished(JobObservable.Ended event) {
			conditionallyRecord(event);
			evictionRecords.add(new EvictionRecord(getJobLocalId(event)));
		}
	}

	class EvictionRecord {
		long localId;

		long evictAt;

		EvictionRecord(long localId) {
			this.localId = localId;
			this.evictAt = System.currentTimeMillis()
					+ Configuration.getLong("evictCompleteJobsAfterMs");
		}

		boolean evict() {
			if (evictAt <= System.currentTimeMillis()) {
				logger.debug("Evict history - job localid {}", localId);
				histories.remove(localId);
				return true;
			} else {
				return false;
			}
		}
	}

	class ToProcessingObserver
			implements ProcessObserver<JobObservable.ToProcessing> {
		@Override
		public void topicPublished(JobObservable.ToProcessing event) {
			conditionallyRecord(event);
		}
	}

	class AllocationThreadStartedObserver
			implements ProcessObserver<JobObservable.AllocationThreadStarted> {
		@Override
		public void
				topicPublished(JobObservable.AllocationThreadStarted event) {
			conditionallyRecord(event);
		}
	}

	class AllocatorCreatedObserver
			implements ProcessObserver<JobObservable.AllocatorCreated> {
		@Override
		public void topicPublished(JobObservable.AllocatorCreated event) {
			conditionallyRecord(event);
		}
	}
}
