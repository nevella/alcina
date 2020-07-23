package cc.alcina.framework.jvmclient.domaintransform;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

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

	@Override
	public boolean isIgnorePropertyChanges() {
		return ignorePropertyChanges.get();
	}

	@Override
	public boolean isReplayingRemoteEvent() {
		return replayingRemoteEvent.get();
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

	@Override
	protected void initCollections() {
		provisionalObjects = Collections
				.synchronizedMap(new IdentityHashMap<>());
	}
}
