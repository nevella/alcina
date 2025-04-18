package cc.alcina.framework.servlet.sync;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.servlet.sync.FlatDeltaPersisterResult.FlatDeltaPersisterResultType;
import cc.alcina.framework.servlet.sync.SyncPair.SyncAction;

/**
 * Persist delta model to the local graph/storage
 * 
 * 
 *
 * @param <D>
 */
public abstract class FlatDeltaPersister<D extends SyncDeltaModel> {
	public static final String CONTEXT_OTHER_OBJECT = FlatDeltaPersister.class
			.getName() + ".CONTEXT_OTHER_OBJECT";

	protected Map<Class, DeltaItemPersister> persisters = new LinkedHashMap<Class, DeltaItemPersister>();

	protected final boolean applyToLeft;

	protected FlatDeltaPersisterResult result;

	protected boolean breakPersistenceForRemoteRefresh;

	Topic<PersistElementResult> topicElementPersisted = Topic.create();

	protected FlatDeltaPersister(boolean applyToLeft) {
		this.applyToLeft = applyToLeft;
	}

	public FlatDeltaPersisterResult apply(Logger logger, D delta,
			List<Class> ignoreDueToIncompleteMerge,
			TopicListener<FlatDeltaPersister.PersistElementResult> persistListener)
			throws Exception {
		if (persistListener != null) {
			topicElementPersisted.add(persistListener);
		}
		try {
			LooseContext.push();
			return apply0(logger, delta, ignoreDueToIncompleteMerge);
		} finally {
			LooseContext.pop();
		}
	}

	private FlatDeltaPersisterResult apply0(Logger logger, D delta,
			List<Class> ignoreDueToIncompleteMerge) throws Exception {
		breakPersistenceForRemoteRefresh = false;
		result = new FlatDeltaPersisterResult();
		result.mergeInterrupted = false;
		for (Class clazz : perClassDeltaOrder()) {
			if (ignoreDueToIncompleteMerge.contains(clazz)) {
				logger.warn(Ax.format("Not persisting - merger %s incomplete",
						clazz.getSimpleName()));
				continue;
			}
			if (breakPersistenceForRemoteRefresh) {
				logger.warn(Ax.format(
						"Not persisting - merger %s - needs remote refresh",
						clazz.getSimpleName()));
				result.mergeInterrupted = true;
				continue;
			}
			FlatDeltaPersisterResult perClassResult = new FlatDeltaPersisterResult();
			DeltaItemPersister persister = persisters.get(clazz);
			List<SyncPair> pairs = delta.getDeltas().getAndEnsure(clazz);
			for (SyncPair pair : pairs) {
				SyncAction syncAction = pair.getAction()
						.getDirectedAction(applyToLeft);
				if (syncAction == null) {
					perClassResult.noModificationCount++;
					result.noModificationCount++;
				} else if (!shouldApply(clazz, pair, syncAction)) {
					perClassResult.noModificationCount++;
					result.noModificationCount++;
				}
			}
			int typeActionIndex = 1;
			int typeActionCount = pairs.size()
					- perClassResult.noModificationCount;
			for (SyncPair pair : pairs) {
				SyncAction syncAction = pair.getAction()
						.getDirectedAction(applyToLeft);
				if (syncAction == null) {
					continue;
				}
				if (!shouldApply(clazz, pair, syncAction)) {
					continue;
				}
				KeyedObject obj = applyToLeft ? pair.getLeft()
						: pair.getRight();
				KeyedObject other = !applyToLeft ? pair.getLeft()
						: pair.getRight();
				LooseContext.set(CONTEXT_OTHER_OBJECT,
						other == null ? null : other.getObject());
				FlatDeltaPersisterResultType resultType = persister
						.performSyncAction(syncAction,
								obj == null ? null : obj.getObject());
				logAction(resultType, obj);
				result.update(resultType);
				perClassResult.update(resultType);
				topicElementPersisted
						.publish(new PersistElementResult(syncAction.toString(),
								objectId(obj), clazz.getSimpleName(),
								typeActionIndex++, typeActionCount));
			}
			classDeltasPersisted();
			Ax.out("Flat delta persister/apply: %s - %s", clazz.getSimpleName(),
					perClassResult);
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

	String objectId(KeyedObject obj) {
		Object object = obj.getObject();
		FormatBuilder format = new FormatBuilder().separator(" - ");
		boolean appended = false;
		if (object instanceof HasId) {
			format.append(((HasId) object).getId());
			appended = true;
		}
		if (object instanceof HasDisplayName) {
			format.append(((HasDisplayName) object).displayName());
			appended = true;
		}
		if (!appended) {
			format.append(object.toString());
		}
		return format.toString();
	}

	protected abstract List<Class> perClassDeltaOrder();

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
		protected List<Class> perClassDeltaOrder() {
			return Arrays.asList();
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

	public static class PersistElementResult {
		public String action;

		public String objectId;

		public String objectType;

		public int typeActionIndex;

		public int typeActionCount;

		public PersistElementResult(String action, String objectId,
				String objectType, int typeActionIndex, int typeActionCount) {
			this.action = action;
			this.objectId = objectId;
			this.objectType = objectType;
			this.typeActionIndex = typeActionIndex;
			this.typeActionCount = typeActionCount;
		}

		@Override
		public String toString() {
			return Ax.format("%s::%s - [%s] - [%s/%s]", objectType, objectId,
					action, typeActionIndex, typeActionCount);
		}
	}
}
