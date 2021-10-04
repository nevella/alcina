package cc.alcina.framework.entity.transform.event;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public class DomainTransformPersistenceEvent {
	private final TransformPersistenceToken transformPersistenceToken;

	private final DomainTransformLayerWrapper domainTransformLayerWrapper;

	private final DomainTransformPersistenceEventType persistenceEventType;

	private boolean firingFromQueue;

	private DomainTransformCommitPosition position;

	private TransformCollation postProcessCollation;

	public long firingStartTime;

	public DomainTransformPersistenceEvent(
			TransformPersistenceToken transformPersistenceToken,
			DomainTransformLayerWrapper domainTransformLayerWrapper,
			DomainTransformPersistenceEventType eventType, boolean localToVm) {
		transformPersistenceToken.setLocalToVm(localToVm);
		this.transformPersistenceToken = transformPersistenceToken;
		this.domainTransformLayerWrapper = domainTransformLayerWrapper;
		this.persistenceEventType = eventType;
	}

	public void ensureTransformsValidForVm() {
		domainTransformLayerWrapper.persistentEvents
				.removeIf(DomainTransformEvent::provideNotApplicableToVmDomain);
	}

	public DomainTransformLayerWrapper getDomainTransformLayerWrapper() {
		return this.domainTransformLayerWrapper;
	}

	public long getMaxPersistedRequestId() {
		return getPersistedRequestIds().stream()
				.collect(Collectors.maxBy(Comparator.naturalOrder()))
				.orElse(0L);
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

	public DomainTransformCommitPosition getPosition() {
		return position;
	}

	// not adjunct - immutable
	public TransformCollation getPostProcessCollation() {
		if (postProcessCollation == null) {
			postProcessCollation = new TransformCollation(
					domainTransformLayerWrapper.persistentEvents);
		}
		return postProcessCollation;
	}

	public TransformPersistenceToken getTransformPersistenceToken() {
		return this.transformPersistenceToken;
	}

	public boolean isFiringFromQueue() {
		return this.firingFromQueue;
	}

	public boolean isLocalToVm() {
		return transformPersistenceToken.isLocalToVm();
	}

	public boolean isPostProcessCascade() {
		return transformPersistenceToken.isLocalToVm()
				|| transformPersistenceToken.isRequestorExternalToThisJvm();
	}

	public void setFiringFromQueue(boolean firingFromQueue) {
		this.firingFromQueue = firingFromQueue;
	}

	public void setPosition(DomainTransformCommitPosition position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return Ax.format("%s\n%s", toStringId(),
				getTransformPersistenceToken());
	}

	public String toStringId() {
		String idString = getMaxPersistedRequestId() == 0 ? Ax.format("%s/%s",
				getTransformPersistenceToken().getRequest().getClientInstance()
						.getId(),
				getTransformPersistenceToken().getRequest().getRequestId())
				: String.valueOf(getMaxPersistedRequestId());
		return Ax.format("DTPE: %s - %s", getPersistenceEventType(), idString);
	}
}