package cc.alcina.framework.servlet.process.observer.job;

import java.util.Objects;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsRemovalEvent;
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
			String state = removalEvent.event.primitiveFieldValues.get("state");
			if (Objects.equals(state, JobState.SEQUENCE_COMPLETE.toString())) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
}
