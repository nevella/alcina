package cc.alcina.framework.servlet.process.observer.mvcc;

import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservable.VersionsRemovalEvent;

public interface ObservableEntityFilter {
	boolean isBeginObservation(VersionsCreationEvent creationEvent);

	// (e.g. when a job becomes 'sequenceComplete')
	boolean isEndObservation(VersionsRemovalEvent removalEvent);
}
