package cc.alcina.framework.servlet.process.observer.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionsCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionsRemovalEvent;
import cc.alcina.framework.servlet.process.observer.mvcc.ObservableEntityFilter;

/**
 * Observes mvcc changes to active job entities
 */
public class JobMvccObserver implements ObservableEntityFilter {
	@Override
	public boolean isBeginObservation(VersionsCreationEvent creationEvent) {
		return creationEvent.event.locator.hasLocalId() && Job.class
				.isAssignableFrom(creationEvent.event.locator.clazz);
	}

	@Override
	public boolean isEndObservation(VersionsRemovalEvent removalEvent) {
		if (Job.class.isAssignableFrom(removalEvent.event.locator.clazz)) {
			Job job = removalEvent.event.locator.find();
			// return job.provideIsSequenceComplete();
			// TODO - queue for ending (rather than end), since the job may
			// still be in flight
			return false;
		}
		return false;
	}
}
