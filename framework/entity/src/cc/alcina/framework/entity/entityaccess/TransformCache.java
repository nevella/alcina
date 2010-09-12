package cc.alcina.framework.entity.entityaccess;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceListener;
/**
 * Note - if you add a per-user class, you'll need to manually invalidate 
 * caching prior to that transform id - or make sure per user class instances are created via the 
 * transform manager
 * 
 * @author nick@alcina.cc
 *
 */
public class TransformCache implements
		DomainTransformRequestPersistenceListener {
	public TransformIdLookup sharedLookup = new TransformIdLookup();

	public Map<Long, TransformIdLookup> perUserLookup = new HashMap<Long, TransformIdLookup>();

	public boolean invalid = false;

	public long cacheValidFrom;

	public Collection<Class> sharedTransformClasses;

	public Collection<Class> perUserTransformClasses;

	public void putSharedTransforms(List<DomainTransformEventPersistent> events) {
		for (DomainTransformEventPersistent event : events) {
			putSharedEvent(event);
		}
		this.cacheValidFrom = CommonUtils.last(events).getId();
	}

	private void putSharedEvent(DomainTransformEventPersistent event) {
		sharedLookup.put(event.getId(), event.toSimpleEvent());
	}

	public void putPerUserTransforms(List<DomainTransformEventPersistent> events) {
		for (DomainTransformEventPersistent event : events) {
			putPerUserEvent(event);
		}
	}

	private void putPerUserEvent(DomainTransformEventPersistent event) {
		long userId = event.getUser().getId();
		if (!perUserLookup.containsKey(userId)) {
			perUserLookup.put(userId, new TransformIdLookup());
		}
		perUserLookup.get(userId).put(event.getId(), event.toSimpleEvent());
	}

	public void onDomainTransformRequestPersistence(
			DomainTransformRequestPersistenceEvent evt) {
		DomainTransformLayerWrapper layerWrapper = evt
				.getDomainTransformLayerWrapper();
		if (layerWrapper != null
				&& layerWrapper.response.getResult() == DomainTransformResponseResult.OK) {
			for (DomainTransformEventPersistent event : layerWrapper.persistentEvents) {
				if (sharedTransformClasses.contains(event.getObjectClass())) {
					putSharedEvent(event);
				} else if (perUserTransformClasses.contains(event
						.getObjectClass())) {
					putPerUserEvent(event);
				}
			}
		}
	}
}
