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

	private long sourceThreadId;

	public DomainTransformPersistenceEvent(
			TransformPersistenceToken transformPersistenceToken,
			DomainTransformLayerWrapper domainTransformLayerWrapper) {
		this(transformPersistenceToken, domainTransformLayerWrapper, Thread
				.currentThread().getId());
	}

	@SuppressWarnings("unchecked")
	public List<Long> getPersistedRequestIds() {
		return domainTransformLayerWrapper == null
				|| domainTransformLayerWrapper.persistentRequests == null ? Collections.EMPTY_LIST
				: EntityUtils
						.hasIdsToIdList(domainTransformLayerWrapper.persistentRequests);
	}

	public DomainTransformPersistenceEvent(
			TransformPersistenceToken transformPersistenceToken,
			DomainTransformLayerWrapper domainTransformLayerWrapper,
			long sourceThreadId) {
		this.sourceThreadId = sourceThreadId;
		this.transformPersistenceToken = transformPersistenceToken;
		this.domainTransformLayerWrapper = domainTransformLayerWrapper;
		persistenceEventType = domainTransformLayerWrapper == null ? DomainTransformPersistenceEventType.PRE_COMMIT
				: domainTransformLayerWrapper.response.getResult() == DomainTransformResponseResult.OK ? DomainTransformPersistenceEventType.COMMIT_OK
						: DomainTransformPersistenceEventType.COMMIT_ERROR;
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public DomainTransformLayerWrapper getDomainTransformLayerWrapper() {
		return this.domainTransformLayerWrapper;
	}

	public DomainTransformPersistenceEventType getPersistenceEventType() {
		return this.persistenceEventType;
	}

	public long getSourceThreadId() {
		return this.sourceThreadId;
	}

	public long getMaxPersistedRequestId() {
		return CommonUtils.lv(CollectionFilters.max(getPersistedRequestIds()));
	}
}