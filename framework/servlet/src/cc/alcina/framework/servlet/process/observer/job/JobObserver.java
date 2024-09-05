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
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;

/**
 * <p>
 * This class gathers detailed logs of the events that occur during the
 * lifecycle of a Job.
 * <p>
 * It has considerable overlap with MvccObserver - if adding any more Observer
 * types, abstract the base.
 */
@Registration.Singleton
public class JobObserver {
	static JobObserver get() {
		return Registry.impl(JobObserver.class);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	public static void observe(ObservableJobFilter filter) {
		get().observe0(filter);
	}

	public static JobHistory getHistory(EntityLocator locator) {
		return get().getHistory0(locator);
	}

	JobHistory getHistory0(EntityLocator locator) {
		return histories.get(locator.localId);
	}

	/*
	 * Since EntityLocator comparison is complex around promoted objects, use
	 * the localId only
	 */
	ConcurrentHashMap<Long, JobHistory> histories = new ConcurrentHashMap<>();

	List<ObservableJobFilter> filters = new CopyOnWriteArrayList<>();

	List<EvictionRecord> evictionRecords = new CopyOnWriteArrayList<>();

	class EvictionRecord {
		EvictionRecord(long localId) {
			this.localId = localId;
			this.evictAt = System.currentTimeMillis()
					+ TimeConstants.ONE_MINUTE_MS;
		}

		long localId;

		long evictAt;

		boolean evict() {
			if (evictAt <= System.currentTimeMillis()) {
				histories.remove(localId);
				return true;
			} else {
				return false;
			}
		}
	}

	void observe0(ObservableJobFilter filter) {
		filters.add(filter);
		if (filters.size() == 1) {
			registerObservers();
		}
	}

	class CreatedObserver implements ProcessObserver<JobObservable.Created> {
		@Override
		public void topicPublished(JobObservable.Created event) {
			if (filters.stream().anyMatch(f -> f.isBeginObservation(event))) {
				if (!isObserving(event)) {
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
		}
	}

	class ToProcessingObserver
			implements ProcessObserver<JobObservable.ToProcessing> {
		@Override
		public void topicPublished(JobObservable.ToProcessing event) {
			conditionallyRecord(event);
		}
	}

	class AllocationEventObserver
			implements ProcessObserver<JobObservable.AllocationEvent> {
		@Override
		public void topicPublished(JobObservable.AllocationEvent event) {
			conditionallyRecord(event);
		}
	}

	void registerObservers() {
		new CreatedObserver().bind();
		new EndedObserver().bind();
		new ToProcessingObserver().bind();
		new AllocationEventObserver().bind();
	}

	boolean isObserving(JobObservable event) {
		long localId = event.job.getLocalId();
		return localId != 0 && histories.containsKey(localId);
	}

	void conditionallyRecord(JobObservable event) {
		JobHistory history = histories.get(event.job.getLocalId());
		if (history != null) {
			history.add(event);
		}
		checkEviction();
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
}
