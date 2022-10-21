package cc.alcina.framework.entity.transform;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;

public class AdjunctTransformCollation extends TransformCollation {
	public static final String CONTEXT_TM_TRANSFORMS_ARE_EX_THREAD = AdjunctTransformCollation.class
			.getName() + ".CONTEXT_TM_TRANSFORMS_ARE_EX_THREAD";

	private TransformPersistenceToken token;

	private boolean applied;

	public AdjunctTransformCollation(
			TransformPersistenceToken transformPersistenceToken) {
		super(transformPersistenceToken.getRequest().allTransforms());
		this.token = transformPersistenceToken;
	}

	/*
	 * Don't perform any modifications in the snapshot - return to base
	 */
	public <T> T callHandleDeleted(QueryResult result, Callable<T> callable) {
		try {
			if (result.hasDeleteTransform()) {
				return Transaction.callInSnapshotTransaction(callable);
			} else {
				return callable.call();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	// this works because of transactions -
	/*
	 * Note that there's a chance some of these requests have already applied -
	 * but that's harmless, as long as we drop the creation events
	 * 
	 * TODO - make this package-private; only expose ensureCurrent
	 */
	public AdjunctTransformCollation ensureApplied() {
		// should only be called by local pre-commit listeners
		Preconditions.checkState(
				Transaction.current().isPreCommit() && token.isLocalToVm());
		if (!applied) {
			applied = true;
			ensureLookups();
			if (!token.isRequestorExternalToThisJvm()
					&& !LooseContext.is(CONTEXT_TM_TRANSFORMS_ARE_EX_THREAD)) {
				// on a normal server-thread pre-commit - these transforms have
				// already been applied to the TLTM
				return this;
			}
			ThreadlocalTransformManager tltm = ThreadlocalTransformManager
					.cast();
			try {
				tltm.setIgnorePropertyChanges(true);
				if (tltm.getClientInstanceEntityMap() == null) {
					tltm.setClientInstanceEntityMap(TransformCommit.get()
							.getLocatorMapForClient(token.getRequest()));
				}
				tltm.setApplyingExternalTransforms(true);
				for (DomainTransformEvent event : allEvents) {
					if (event
							.getTransformType() == TransformType.CREATE_OBJECT) {
						// only create our transient event receiver if this
						// request hasn't been applied
						if (tltm.getClientInstanceEntityMap().getForLocalId(
								event.getObjectLocalId()) == null) {
							Entity instance = (Entity) tltm.newInstance(
									event.getObjectClass(), event.getObjectId(),
									event.getObjectLocalId(), true);
							token.getTargetStore().putExternalLocal(instance);
						}
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
		return this;
	}

	/**
	 * @return true if transforms were added to the wrapped request
	 */
	public boolean ensureCurrent() {
		ensureApplied();
		if (token.addCascadedEvents()) {
			refreshFromRequest();
			removeNonPersistentTransforms();
			return true;
		} else {
			return false;
		}
	}

	public void refreshFromRequest() {
		refresh(token.getRequest().allTransforms());
	}

	public void removeNonPersistentTransforms() {
		ensureLookups();
		AtomicBoolean modified = new AtomicBoolean();
		allEntityCollations().forEach(ec -> {
			if (ec.isCreatedAndDeleted()) {
				ec.getTransforms().forEach(this::removeTransformFromRequest);
				ec.getValueTransforms()
						.forEach(this::removeTransformFromRequest);
				modified.set(true);
			} else {
				ec.ensureByPropertyName().values().forEach(list -> {
					for (int idx = 0; idx < list.size() - 1; idx++) {
						DomainTransformEvent transform = list.get(idx);
						if (transform.getTransformType()
								.isNotCollectionTransform()) {
							removeTransformFromRequest(transform);
							modified.set(true);
						}
					}
				});
			}
		});
		if (modified.get()) {
			refreshFromRequest();
		}
	}

	@Override
	public void removeTransformFromRequest(DomainTransformEvent event) {
		Preconditions.checkState(token.getTransformResult() == null);
		token.getRequest().removeTransform(event);
	}
}
