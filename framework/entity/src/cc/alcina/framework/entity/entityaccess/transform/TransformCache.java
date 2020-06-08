package cc.alcina.framework.entity.entityaccess.transform;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceListener;

/**
 * Note - if you add a per-user class, you'll need to manually invalidate
 * caching prior to that transform id - or make sure per user class instances
 * are created via the transform manager
 * 
 * @author nick@alcina.cc
 * 
 */
public class TransformCache implements DomainTransformPersistenceListener {
	public TransformIdLookup sharedLookup = new TransformIdLookup();

	public Map<Long, TransformIdLookup> perUserLookup = new HashMap<Long, TransformIdLookup>();

	public boolean invalid = false;

	public long cacheValidFrom;

	public Collection<Class> sharedTransformClasses;

	public Collection<Class> perUserTransformClasses;

	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent evt) {
		DomainTransformLayerWrapper layerWrapper = evt
				.getDomainTransformLayerWrapper();
		if (layerWrapper != null && layerWrapper.response
				.getResult() == DomainTransformResponseResult.OK) {
			for (DomainTransformEventPersistent event : layerWrapper.persistentEvents) {
				if (sharedTransformClasses.contains(event.getObjectClass())) {
					putSharedEvent(event);
				} else if (perUserTransformClasses
						.contains(event.getObjectClass())) {
					putPerUserEvent(event);
				}
			}
		}
	}

	public void
			putPerUserTransforms(List<DomainTransformEventPersistent> events) {
		for (DomainTransformEventPersistent event : events) {
			putPerUserEvent(event);
		}
	}

	public void
			putSharedTransforms(List<DomainTransformEventPersistent> events) {
		for (DomainTransformEventPersistent event : events) {
			putSharedEvent(event);
		}
		this.cacheValidFrom = CommonUtils.last(events).getId();
	}

	private void putPerUserEvent(DomainTransformEventPersistent event) {
		long userId = event.getUser().getId();
		if (!perUserLookup.containsKey(userId)) {
			perUserLookup.put(userId, new TransformIdLookup());
		}
		DomainTransformEvent nonPersistentEvent = event
				.toNonPersistentEvent(true);
		perUserLookup.get(userId).put(event.getId(), nonPersistentEvent);
	}

	private void putSharedEvent(DomainTransformEventPersistent event) {
		DomainTransformEvent nonPersistentEvent = event
				.toNonPersistentEvent(true);
		sharedLookup.put(event.getId(), nonPersistentEvent);
	}
}
