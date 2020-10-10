package cc.alcina.framework.entity.transform.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public class DomainTransformPersistenceEvent {
	private final TransformPersistenceToken transformPersistenceToken;

	private final DomainTransformLayerWrapper domainTransformLayerWrapper;

	private final DomainTransformPersistenceEventType persistenceEventType;

	private List<Runnable> postEventRunnables = new ArrayList<>();

	public DomainTransformPersistenceEvent(
			TransformPersistenceToken transformPersistenceToken,
			DomainTransformLayerWrapper domainTransformLayerWrapper,
			boolean localToVm) {
		transformPersistenceToken.setLocalToVm(localToVm);
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
				.removeIf(DomainTransformEvent::provideNotApplicableToVmDomain);
	}

	public DomainTransformLayerWrapper getDomainTransformLayerWrapper() {
		return this.domainTransformLayerWrapper;
	}

	public long getMaxPersistedRequestId() {
		return CommonUtils.lv(CollectionFilters.max(getPersistedRequestIds()));
	}

	public List<Long> getPersistedRequestIds() {
		return getPersistedRequests().stream().map(HasId::getId)
				.collect(Collectors.toList());
	}

	public List<DomainTransformRequestPersistent> getPersistedRequests() {
		return domainTransformLayerWrapper == null
				|| domainTransformLayerWrapper.persistentRequests == null
						? Collections.EMPTY_LIST
						: domainTransformLayerWrapper.persistentRequests;
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
		return transformPersistenceToken.isLocalToVm();
	}

	public void setPostEventRunnables(List<Runnable> postEventRunnables) {
		this.postEventRunnables = postEventRunnables;
	}
}