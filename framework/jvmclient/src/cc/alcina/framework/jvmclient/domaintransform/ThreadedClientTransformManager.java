package cc.alcina.framework.jvmclient.domaintransform;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;

@SuppressWarnings("unchecked")
public class ThreadedClientTransformManager extends
		ClientTransformManagerCommon {
	private static ThreadLocal<Boolean> ignorePropertyChanges = new ThreadLocal() {
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	private static ThreadLocal<Boolean> replayingRemoteEvent = new ThreadLocal() {
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

	public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
			long id, long localId) {
		if (this.getDomainObjects() != null) {
			T object = getDomainObjects().getObject(c, id, localId);
			if (object == null && localId != 0 && id == 0) {
				HiliLocator hiliLocator = userSessionHiliMap.get(localId);
				if (hiliLocator != null) {
					return getDomainObjects().getObject(c, hiliLocator.getId(),
							0L);
				}
			}else{
				return object;
			}
		}
		return null;
	}

	HiliLocatorMap userSessionHiliMap = new HiliLocatorMap();

	public synchronized <H extends HasIdAndLocalId> long getLocalIdForClientInstance(H hili) {
		return userSessionHiliMap.getLocalIdForClientInstance(hili);
	}

	public synchronized void registerHiliMappingPriorToLocalIdDeletion(Class clazz, long id,
			long localId) {
		userSessionHiliMap.putToLookups(new HiliLocator(clazz, id, localId));
	}
}
