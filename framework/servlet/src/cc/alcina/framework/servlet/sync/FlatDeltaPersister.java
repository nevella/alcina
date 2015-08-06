package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.Map;

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

	protected abstract Class[] perClassDeltaOrder();

	public FlatDeltaPersisterResult apply(D delta) throws Exception {
		FlatDeltaPersisterResult result = new FlatDeltaPersisterResult();
		for (Class clazz : perClassDeltaOrder()) {
			FlatDeltaPersisterResult perClassResult = new FlatDeltaPersisterResult();
			DeltaItemPersister persister = persisters.get(clazz);
			for (SyncPair pair : delta.getDeltas().getAndEnsure(clazz)) {
				SyncAction syncAction = pair.getAction().getDirectedAction(
						applyLeft);
				if (syncAction == null) {
					result.noModificationCount++;
					continue;
				}
				if (!shouldApply(clazz, pair, syncAction)) {
					result.noModificationCount++;
					continue;
				}
				KeyedObject obj = applyLeft ? pair.getLeft() : pair.getRight();
				FlatDeltaPersisterResultType resultType = persister
						.performSyncAction(syncAction,
								obj == null ? null : obj.getObject());
				logAction(resultType, obj);
				result.update(resultType);
				perClassResult.update(resultType);
			}
			System.out.format("Flat delta persister/apply: %s - %s\n",
					clazz.getSimpleName(), perClassResult);
		}
		return result;
	}

	protected void logAction(FlatDeltaPersisterResultType resultType,
			Object object) {
		switch (resultType) {
		case CREATED:
			System.out.println("create -> " + object);
			break;
		case MERGED:
			System.out.println("updated -> " + object);
			break;
		}
	}

	protected abstract boolean shouldApply(Class interchangeClass,
			SyncPair pair, SyncAction syncAction);

	public static class NullDeltaPersister extends FlatDeltaPersister {
		public NullDeltaPersister(boolean applyLeft) {
			super(applyLeft);
		}

		@Override
		protected Class[] perClassDeltaOrder() {
			return new Class[0];
		}

		@Override
		protected boolean shouldApply(Class interchangeClass, SyncPair pair,
				SyncAction syncAction) {
			return false;
		}
	}
}
