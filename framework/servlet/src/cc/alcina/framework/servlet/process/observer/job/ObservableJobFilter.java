package cc.alcina.framework.servlet.process.observer.job;

import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable.Created;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable.Ended;

public interface ObservableJobFilter {
	boolean isBeginObservation(JobObservable.Created creationEvent);

	// (e.g. when a job becomes 'sequenceComplete')
	boolean isEndObservation(JobObservable.Ended endedEvent);

	public static class All implements ObservableJobFilter {
		int begunCount = 0;

		@Override
		public synchronized boolean isBeginObservation(Created creationEvent) {
			if (begunCount > 0) {
				if (Configuration.is("firstJobOnly")) {
					return false;
				}
			}
			begunCount++;
			return true;
		}

		@Override
		public boolean isEndObservation(Ended endedEvent) {
			return true;
		}
	}
}
