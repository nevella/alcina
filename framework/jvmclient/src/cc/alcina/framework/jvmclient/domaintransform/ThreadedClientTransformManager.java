package cc.alcina.framework.jvmclient.domaintransform;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;


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
    public synchronized <H extends Entity> long getLocalIdForClientInstance(
            H entity) {
        return userSessionEntityMap.getLocalIdForClientInstance(entity);
    }

    @Override
    public <T extends Entity> T getObject(Class<? extends T> c,
            long id, long localId) {
        if (this.getDomainObjects() != null) {
            T object = getDomainObjects().getObject(c, id, localId);
            if (object == null && localId != 0 && id == 0) {
                EntityLocator hiliLocator = userSessionEntityMap
                        .getForLocalId(localId);
                if (hiliLocator != null) {
                    return getDomainObjects().getObject(c, hiliLocator.getId(),
                            0L);
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
    public synchronized void registerEntityMappingPriorToLocalIdDeletion(
            Class clazz, long id, long localId) {
        userSessionEntityMap.putToLookups(new EntityLocator(clazz, id, localId));
    }

    @Override
    public void setIgnorePropertyChanges(boolean _ignorePropertyChanges) {
        ignorePropertyChanges.set(_ignorePropertyChanges);
    }

    @Override
    public void setReplayingRemoteEvent(boolean _replayingRemoteEvent) {
        replayingRemoteEvent.set(_replayingRemoteEvent);
    }
}
