package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.CRC32;

import cc.alcina.framework.servlet.sync.FlatDeltaPersisterResult.FlatDeltaPersisterResultType;
import cc.alcina.framework.servlet.sync.SyncPair.SyncAction;

public abstract class FlatDeltaPersister<D extends SyncDeltaModel, E extends SyncEndpointModel> {
	public static interface DeltaItemPersister<C> {
		FlatDeltaPersisterResultType performSyncAction(SyncAction syncAction,
				C object) throws Exception;
	}

	protected Map<Class, DeltaItemPersister> persisters = new LinkedHashMap<Class, DeltaItemPersister>();

	protected final boolean applyLeft;

	protected FlatDeltaPersister(boolean applyLeft) {
		this.applyLeft = applyLeft;
	}

	public FlatDeltaPersisterResult apply(D delta) throws Exception {
		FlatDeltaPersisterResult result = new FlatDeltaPersisterResult();
		for (Entry<Class, List<SyncPair>> e : delta.getDeltas().entrySet()) {
			DeltaItemPersister persister = persisters.get(e.getKey());
			for (SyncPair pair : e.getValue()) {
				SyncAction syncAction = pair.getAction().getDirectedAction(
						applyLeft);
				if (syncAction == null) {
					result.noModificationCount++;
					continue;
				}
				if (!shouldApply(e.getKey(), syncAction)) {
					result.noModificationCount++;
					continue;
				}
				KeyedObject obj = applyLeft ? pair.getLeft() : pair.getRight();
				FlatDeltaPersisterResultType resultType = persister
						.performSyncAction(syncAction,
								obj == null ? null : obj.getObject());
				result.update(resultType);
			}
		}
		return result;
	}

	protected abstract boolean shouldApply(Class interchangeClass,
			SyncAction syncAction);
}
