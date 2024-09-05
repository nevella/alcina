package cc.alcina.framework.servlet.process.observer.job;

import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable.Created;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable.Ended;

public interface ObservableJobFilter {
	boolean isBeginObservation(JobObservable.Created creationEvent);

	// (e.g. when a job becomes 'sequenceComplete')
	boolean isEndObservation(JobObservable.Ended endedEvent);

	public static class All implements ObservableJobFilter {
		@Override
		public boolean isBeginObservation(Created creationEvent) {
			return true;
		}

		@Override
		public boolean isEndObservation(Ended endedEvent) {
			return true;
		}
	}
}
