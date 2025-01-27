package cc.alcina.framework.entity.persistence.transform;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.policy.TransformPropagationPolicy;

class PersistentEventPopulator {
	void populate(IUser originatingUser,
			List<DomainTransformEventPersistent> persistentEvents,
			ThreadlocalTransformManager tltm,
			List<DomainTransformEvent> eventsPersisted,
			TransformPropagationPolicy propagationPolicy,
			Class<? extends DomainTransformEventPersistent> persistentEventClass,
			DomainTransformRequestPersistent persistentRequest,
			AtomicBoolean missingClassRefWarned,
			boolean persistTransformsDisabled, boolean forcePropagation) {
		boolean pushed = false;
		try {
			if (originatingUser != null) {
				PermissionsManager.get().pushUser(originatingUser,
						PermissionsManager.get().getLoginState());
				pushed = true;
			}
			populate0(persistentEvents, tltm, eventsPersisted,
					propagationPolicy, persistentEventClass, persistentRequest,
					missingClassRefWarned,
					LooseContext.is(
							TransformPersisterInPersistenceContext.CONTEXT_DO_NOT_PERSIST_TRANSFORMS),
					false);
		} finally {
			if (pushed) {
				PermissionsManager.get().popUser();
			}
		}
	}

	private void populate0(
			List<DomainTransformEventPersistent> persistentEvents,
			ThreadlocalTransformManager tltm,
			List<DomainTransformEvent> eventsPersisted,
			TransformPropagationPolicy propagationPolicy,
			Class<? extends DomainTransformEventPersistent> persistentEventClass,
			DomainTransformRequestPersistent persistentRequest,
			AtomicBoolean missingClassRefWarned,
			boolean persistTransformsDisabled, boolean forcePropagation) {
		TransformCollation collation = new TransformCollation(eventsPersisted);
		Map<EntityLocator, DomainTransformEventPersistent> mostRecentWithMetadata = new LinkedHashMap<>();
		for (DomainTransformEvent event : eventsPersisted) {
			DomainTransformEventPersistent propagationEvent = Reflections
					.newInstance(persistentEventClass);
			propagationEvent.wrap(event);
			/*
			 * Remove all non-propagatable refs (they'll be confusing for local
			 * commits)
			 */
			propagationEvent.setSource(null);
			propagationEvent.setNewValue(null);
			propagationEvent.setOldValue(null);
			if (event.getTransformType() != TransformType.DELETE_OBJECT
					&& propagationPolicy.handlesEvent(event)) {
				propagationEvent.populateDbMetadata(event);
				DomainTransformEventPersistent mostRecent = mostRecentWithMetadata
						.get(event.toObjectLocator());
				if (mostRecent != null) {
					mostRecent.setExTransformDbMetadata(null);
				}
				mostRecentWithMetadata.put(event.toObjectLocator(), mostRecent);
			}
			if (propagationEvent.getObjectClassRef() == null
					&& !missingClassRefWarned.get()) {
				missingClassRefWarned.set(true);
				System.out.println(
						"Warning - persisting transform without a classRef - "
								+ propagationEvent);
			}
			if (propagationEvent.getObjectId() == 0) {
				propagationEvent
						.setObjectId(tltm.getObjectStore()
								.getObject(propagationEvent.getObjectClass(), 0,
										propagationEvent.getObjectLocalId())
								.getId());
			}
			if (propagationEvent.getValueId() == 0
					&& propagationEvent.getValueLocalId() != 0) {
				propagationEvent
						.setValueId(tltm.getObjectStore()
								.getObject(propagationEvent.getValueClass(), 0,
										propagationEvent.getValueLocalId())
								.getId());
			}
			propagationEvent.setServerCommitDate(new Date());
			if (propagationPolicy.shouldPropagate(propagationEvent)
					|| forcePropagation) {
				// note that this won't persist the 'persistent'
				// event if propgationType=NON_PERSISTENT
				propagationEvent
						.setDomainTransformRequestPersistent(persistentRequest);
				persistentRequest.getEvents().add(propagationEvent);
				persistentEvents.add(propagationEvent);
			}
			/*
			 * persist at end (no double-calls)
			 */
			if (propagationPolicy.shouldPersistEventRecord(event)
					|| Configuration.is(
							TransformPersisterInPersistenceContext.class,
							"persistAllTransforms")) {
				if (!persistTransformsDisabled) {
					tltm.persist(propagationEvent);
				}
			}
		}
	}
}
