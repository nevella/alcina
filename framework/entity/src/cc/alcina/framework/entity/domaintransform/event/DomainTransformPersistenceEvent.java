package cc.alcina.framework.entity.domaintransform.event;

import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.projection.EntityUtils;

public class DomainTransformPersistenceEvent {
	private final TransformPersistenceToken transformPersistenceToken;

	private final DomainTransformLayerWrapper domainTransformLayerWrapper;

	private final DomainTransformPersistenceEventType persistenceEventType;

	private boolean localToVm;

	public DomainTransformPersistenceEvent(
			TransformPersistenceToken transformPersistenceToken,
			DomainTransformLayerWrapper domainTransformLayerWrapper,
			boolean localToVm) {
		this.localToVm = localToVm;
		this.transformPersistenceToken = transformPersistenceToken;
		this.domainTransformLayerWrapper = domainTransformLayerWrapper;
		persistenceEventType = domainTransformLayerWrapper == null ? DomainTransformPersistenceEventType.PRE_COMMIT
				: domainTransformLayerWrapper.response.getResult() == DomainTransformResponseResult.OK ? DomainTransformPersistenceEventType.COMMIT_OK
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

	@SuppressWarnings("unchecked")
	public List<Long> getPersistedRequestIds() {
		return domainTransformLayerWrapper == null
				|| domainTransformLayerWrapper.persistentRequests == null ? Collections.EMPTY_LIST
				: EntityUtils
						.hasIdsToIdList(domainTransformLayerWrapper.persistentRequests);
	}

	public DomainTransformPersistenceEventType getPersistenceEventType() {
		return this.persistenceEventType;
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public boolean isLocalToVm() {
		return this.localToVm;
	}
}