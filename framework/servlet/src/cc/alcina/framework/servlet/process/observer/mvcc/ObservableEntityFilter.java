package cc.alcina.framework.servlet.process.observer.mvcc;

import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionsCreationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables.VersionsRemovalEvent;

public interface ObservableEntityFilter {
	boolean isBeginObservation(VersionsCreationEvent creationEvent);

	// (e.g. when a job becomes 'sequenceComplete')
	boolean isEndObservation(VersionsRemovalEvent removalEvent);
}
