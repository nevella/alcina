package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.servlet.sync.FlatDeltaPersister.DeltaItemPersister;
import cc.alcina.framework.servlet.sync.SyncPair.SyncAction;

public class LocalDomainLocatablePersister<T extends AbstractLocalDomainLocatable>
		implements DeltaItemPersister<T> {
	public LocalDomainLocatablePersister() {
	}

	@Override
	public boolean performSyncAction(SyncAction syncAction, T object)
			throws Exception {
		switch (syncAction) {
		case DELETE:
			object.deleteLocalEquivalent();
			return true;
		case CREATE:
			return object.ensureLocalEquivalent() != null;
		case UPDATE:
			return object.updateLocalEquivalent() != null;
		}
		return false;
	}
}