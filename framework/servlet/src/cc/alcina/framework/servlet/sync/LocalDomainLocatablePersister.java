package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.sync.AbstractTypedLocalDomainLocatable;
import cc.alcina.framework.servlet.sync.FlatDeltaPersister.DeltaItemPersister;
import cc.alcina.framework.servlet.sync.FlatDeltaPersisterResult.FlatDeltaPersisterResultType;
import cc.alcina.framework.servlet.sync.SyncPair.SyncAction;

public class LocalDomainLocatablePersister<T extends AbstractTypedLocalDomainLocatable>
		implements DeltaItemPersister<T> {
	public LocalDomainLocatablePersister() {
	}

	@Override
	public FlatDeltaPersisterResultType performSyncAction(SyncAction syncAction,
			T object) throws Exception {
		switch (syncAction) {
		case DELETE:
			object.deleteLocalEquivalent();
			return FlatDeltaPersisterResultType.DELETED;
		case CREATE:
			if (object == null) {
				System.err.println("Create with null object");
				return FlatDeltaPersisterResultType.UNMATCHED;
			}
			return object.ensureLocalEquivalent() == null
					? FlatDeltaPersisterResultType.UNMATCHED
					: FlatDeltaPersisterResultType.CREATED;
		case UPDATE:
			return object.updateLocalEquivalent() == null
					? FlatDeltaPersisterResultType.UNMODIFIED
					: FlatDeltaPersisterResultType.MERGED;
		}
		return FlatDeltaPersisterResultType.UNMATCHED;
	}
}