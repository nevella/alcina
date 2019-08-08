package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.cache.DomainModificationMetadataProvider;
import cc.alcina.framework.entity.projection.EntityUtils;

public class DomainTransformPersistenceEvent {
    private final TransformPersistenceToken transformPersistenceToken;

    private final DomainTransformLayerWrapper domainTransformLayerWrapper;

    private final DomainTransformPersistenceEventType persistenceEventType;

    private List<Runnable> postEventRunnables = new ArrayList<>();

    private boolean localToVm;

    private DomainModificationMetadataProvider metadataProvider;

    public DomainTransformPersistenceEvent(
            TransformPersistenceToken transformPersistenceToken,
            DomainTransformLayerWrapper domainTransformLayerWrapper,
            boolean localToVm) {
        this.localToVm = localToVm;
        this.transformPersistenceToken = transformPersistenceToken;
        this.domainTransformLayerWrapper = domainTransformLayerWrapper;
        persistenceEventType = domainTransformLayerWrapper == null
                ? DomainTransformPersistenceEventType.PRE_COMMIT
                : domainTransformLayerWrapper.response
                        .getResult() == DomainTransformResponseResult.OK
                                ? DomainTransformPersistenceEventType.COMMIT_OK
                                : DomainTransformPersistenceEventType.COMMIT_ERROR;
    }

    public void ensureTransformsValidForVm() {
        domainTransformLayerWrapper.persistentEvents
                .removeIf(evt -> evt.getObjectClassRef().notInVm()
                        || (evt.getValueClassRef() != null
                                && evt.getValueClassRef().notInVm()));
    }

    public DomainTransformLayerWrapper getDomainTransformLayerWrapper() {
        return this.domainTransformLayerWrapper;
    }

    public long getMaxPersistedRequestId() {
        return CommonUtils.lv(CollectionFilters.max(getPersistedRequestIds()));
    }

    public DomainModificationMetadataProvider getMetadataProvider() {
        return this.metadataProvider;
    }

    public List<Long> getPersistedRequestIds() {
        return domainTransformLayerWrapper == null
                || domainTransformLayerWrapper.persistentRequests == null
                        ? Collections.EMPTY_LIST
                        : EntityUtils.hasIdsToIdList(
                                domainTransformLayerWrapper.persistentRequests);
    }

    public DomainTransformPersistenceEventType getPersistenceEventType() {
        return this.persistenceEventType;
    }

    public List<Runnable> getPostEventRunnables() {
        return this.postEventRunnables;
    }

    public TransformPersistenceToken getTransformPersistenceToken() {
        return this.transformPersistenceToken;
    }

    public boolean isLocalToVm() {
        return this.localToVm;
    }

    public void setMetadataProvider(
            DomainModificationMetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    public void setPostEventRunnables(List<Runnable> postEventRunnables) {
        this.postEventRunnables = postEventRunnables;
    }
}