package cc.alcina.framework.entity.domaintransform;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit;

public class AdjunctTransformCollation extends TransformCollation {
	private TransformPersistenceToken token;

	private boolean applied;

	public AdjunctTransformCollation(
			TransformPersistenceToken transformPersistenceToken) {
		super(transformPersistenceToken.getRequest().allTransforms());
		this.token = transformPersistenceToken;
		this.applied = transformPersistenceToken.isLocalToVm();
	}

	// this works because of transactions -
	/*
	 * Note that there's a chance some of these requests have already applied -
	 * but that's harmless, as long as we drop the creation events
	 */
	public void ensureApplied() {
		if (!applied) {
			applied = true;
			ensureLookups();
			if (!token.isLocalToVm() || !token.isAsyncClient()) {
				return;
			}
			ThreadlocalTransformManager tltm = ThreadlocalTransformManager
					.cast();
			try {
				tltm.setIgnorePropertyChanges(true);
				if (tltm.getUserSessionEntityMap() == null) {
					tltm.setUserSessionEntityMap(TransformCommit.get()
							.getLocatorMapForClient(token.getRequest()));
				}
				tltm.setApplyingExternalTransforms(true);
				// FIXME - mvcc.3 - index
				for (DomainTransformEvent event : allEvents) {
					if (event
							.getTransformType() == TransformType.CREATE_OBJECT) {
						// only create our transient event receiver if this
						// request hasn't been applied
						if (tltm.getUserSessionEntityMap().getForLocalId(
								event.getObjectLocalId()) == null) {
							Entity instance = (Entity) tltm.newInstance(
									event.getObjectClass(), event.getObjectId(),
									event.getObjectLocalId());
							token.getTargetStore().putExternalLocal(instance);
						}
					}
					if (WrapperPersistable.class
							.isAssignableFrom(event.getObjectClass())) {
						continue;
					}
					TransformManager.get().apply(event);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				tltm.setApplyingExternalTransforms(false);
				tltm.setIgnorePropertyChanges(false);
			}
		}
	}

	@Override
	protected void removeTransformsFromRequest(QueryResult queryResult) {
		Preconditions.checkState(token.getTransformResult() == null);
		queryResult.events.forEach(token.getRequest()::removeTransform);
	}
}
