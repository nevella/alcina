package cc.alcina.framework.jvmclient.domaintransform;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;

/**
 * <h2>Thread-safety notes</h2>
 * <ul>
 * <li>EntityLocatorMap is thread-safe
 * <li>QueueingFinished timer only accessed in synchronized methods, ditto
 * localRequestId ditto committingRequest
 * </ul>
 * 
 * @author nick@alcina.cc
 *
 */
public class ThreadedClientTransformManager
		extends ClientTransformManagerCommon {
	private static ThreadLocal<Boolean> ignorePropertyChanges = new ThreadLocal() {
		@Override
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	private static ThreadLocal<Boolean> replayingRemoteEvent = new ThreadLocal() {
		@Override
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	EntityLocatorMap userSessionEntityMap = new EntityLocatorMap();

	@Override
	public <H extends Entity> long getLocalIdForClientInstance(H entity) {
		return userSessionEntityMap.getLocalIdForClientInstance(entity);
	}

	@Override
	public synchronized <T extends Entity> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (this.getDomainObjects() != null) {
			T object = getDomainObjects().getObject(c, id, localId);
			if (object == null && localId != 0 && id == 0) {
				EntityLocator entityLocator = userSessionEntityMap
						.getForLocalId(localId);
				if (entityLocator != null) {
					return getDomainObjects().getObject(c,
							entityLocator.getId(), 0L);
				}
			} else {
				return object;
			}
		}
		return null;
	}

	@Override
	public boolean isIgnorePropertyChanges() {
		return ignorePropertyChanges.get();
	}

	@Override
	public boolean isReplayingRemoteEvent() {
		return replayingRemoteEvent.get();
	}

	@Override
	protected void initCollections() {
		provisionalObjects = Collections
				.synchronizedMap(new IdentityHashMap<>());
	}

	@Override
	public synchronized void registerEntityMappingPriorToLocalIdDeletion(
			Class clazz, long id, long localId) {
		userSessionEntityMap
				.putToLookups(new EntityLocator(clazz, id, localId));
	}

	@Override
	public void setIgnorePropertyChanges(boolean _ignorePropertyChanges) {
		ignorePropertyChanges.set(_ignorePropertyChanges);
	}

	@Override
	public void setReplayingRemoteEvent(boolean _replayingRemoteEvent) {
		replayingRemoteEvent.set(_replayingRemoteEvent);
	}

	@Override
	protected Set<DomainTransformEvent> createTransformSet() {
		return Collections.synchronizedSet(super.createTransformSet());
	}
	/*
	 * all methods that return collections, check iteration
	 */
}
