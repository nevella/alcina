package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

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
		Ax.out(">>> ntnt:: %s",
				domainTransformLayerWrapper.persistentEvents.size());
		domainTransformLayerWrapper.persistentEvents
				.removeIf(DomainTransformEvent::provideNotApplicableToVmDomain);
		Ax.out(">>> ntnt:: %s",
				domainTransformLayerWrapper.persistentEvents.size());
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