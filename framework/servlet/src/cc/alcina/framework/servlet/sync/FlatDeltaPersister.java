package cc.alcina.framework.servlet.sync;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.sync.FlatDeltaPersisterResult.FlatDeltaPersisterResultType;
import cc.alcina.framework.servlet.sync.SyncPair.SyncAction;

/**
 * Persist delta model to the local graph/storage
 * 
 * @author nick@alcina.cc
 *
 * @param <D>
 */
public abstract class FlatDeltaPersister<D extends SyncDeltaModel> {
	public static final String CONTEXT_OTHER_OBJECT = FlatDeltaPersister.class.getName()+".CONTEXT_OTHER_OBJECT";

	protected Map<Class, DeltaItemPersister> persisters = new LinkedHashMap<Class, DeltaItemPersister>();

	protected final boolean applyToLeft;

	protected FlatDeltaPersisterResult result;

	protected FlatDeltaPersister(boolean applyToLeft) {
		this.applyToLeft = applyToLeft;
	}
	protected boolean breakPersistenceForRemoteRefresh;
	public FlatDeltaPersisterResult apply(Logger logger, D delta,
			List<Class> ignoreDueToIncompleteMerge) throws Exception {
		try {
			LooseContext.push();
			return apply0(logger, delta, ignoreDueToIncompleteMerge);
		} finally {
			LooseContext.pop();
		}
	}
	private FlatDeltaPersisterResult apply0(Logger logger, D delta,
			List<Class> ignoreDueToIncompleteMerge) throws Exception {
		breakPersistenceForRemoteRefresh=false;
		result = new FlatDeltaPersisterResult();
		result.mergeInterrupted=false;
		for (Class clazz : perClassDeltaOrder()) {
			if (ignoreDueToIncompleteMerge.contains(clazz)) {
				logger.warn(Ax.format("Not persisting - merger %s incomplete",
						clazz.getSimpleName()));
				continue;
			}
			if(breakPersistenceForRemoteRefresh){
				logger.warn(Ax.format("Not persisting - merger %s - needs remote refresh",
						clazz.getSimpleName()));
				result.mergeInterrupted=true;
				continue;
			}
			FlatDeltaPersisterResult perClassResult = new FlatDeltaPersisterResult();
			DeltaItemPersister persister = persisters.get(clazz);
			for (SyncPair pair : delta.getDeltas().getAndEnsure(clazz)) {
				SyncAction syncAction = pair.getAction()
						.getDirectedAction(applyToLeft);
				if (syncAction == null) {
					perClassResult.noModificationCount++;
					result.noModificationCount++;
					continue;
				}
				if (!shouldApply(clazz, pair, syncAction)) {
					perClassResult.noModificationCount++;
					result.noModificationCount++;
					continue;
				}
				KeyedObject obj = applyToLeft ? pair.getLeft()
						: pair.getRight();
				KeyedObject other = !applyToLeft ? pair.getLeft()
						: pair.getRight();
				LooseContext.set(CONTEXT_OTHER_OBJECT, other.getObject());
				FlatDeltaPersisterResultType resultType = persister
						.performSyncAction(syncAction,
								obj == null ? null : obj.getObject());
				logAction(resultType, obj);
				result.update(resultType);
				perClassResult.update(resultType);
			}
			classDeltasPersisted();
			JobRegistry.get().log("Flat delta persister/apply: %s - %s",
					clazz.getSimpleName(), perClassResult);
		}
		return result;
	}

	protected void classDeltasPersisted() {
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
		case DELETED:
			System.out.println("deleted -> " + object);
			break;
		}
	}

	protected abstract Class[] perClassDeltaOrder();

	protected abstract boolean shouldApply(Class interchangeClass,
			SyncPair pair, SyncAction syncAction);

	public static interface DeltaItemPersister<C> {
		FlatDeltaPersisterResultType performSyncAction(SyncAction syncAction,
				C object) throws Exception;
	}

	public static class DeltaItemPersisterNull<C>
			implements DeltaItemPersister<C> {
		@Override
		public FlatDeltaPersisterResultType performSyncAction(
				SyncAction syncAction, C object) throws Exception {
			return FlatDeltaPersisterResultType.UNMODIFIED;
		}
	}

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
			System.out.format("Would persist - %s :: %s :: %s\n",
					interchangeClass.getSimpleName(), pair.getKey(),
					syncAction);
			return false;
		}
	}
}
