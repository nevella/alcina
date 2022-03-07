package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Bean
public class DomainTranche<T extends DomainModelObject>
		implements DomainModelDelta, Serializable {
	private DomainModelHolder domainModelHolder;

	private Collection<DomainModelDeltaEntity> unlinkedObjects = new ArrayList<DomainModelDeltaEntity>();

	private Collection<DomainTransformEvent> replayEvents = new ArrayList<DomainTransformEvent>();

	private String appInstruction;

	private DomainModelDeltaSignature signature;

	private DomainModelDeltaMetadata metadata;

	private T domainModelObject;

	@Override
	public String getAppInstruction() {
		return this.appInstruction;
	}

	@Override
	public DomainModelHolder getDomainModelHolder() {
		return this.domainModelHolder;
	}

	@Override
	public T getDomainModelObject() {
		return this.domainModelObject;
	}

	@Override
	public DomainModelDeltaMetadata getMetadata() {
		return metadata;
	}

	@Override
	public Collection<DomainTransformEvent> getReplayEvents() {
		return this.replayEvents;
	}

	@Override
	public DomainModelDeltaSignature getSignature() {
		return signature;
	}

	@Override
	public Collection<DomainModelDeltaEntity> getUnlinkedObjects() {
		return this.unlinkedObjects;
	}

	@Override
	public boolean hasLocalOnlyTransforms() {
		return false;
	}

	public void merge(DomainTranche other) {
		unlinkedObjects.addAll(other.getUnlinkedObjects());
		replayEvents.addAll(other.getReplayEvents());
		signature = other.signature;
	}

	public void setAppInstruction(String appInstruction) {
		this.appInstruction = appInstruction;
	}

	public void setDomainModelHolder(DomainModelHolder domainModelHolder) {
		this.domainModelHolder = domainModelHolder;
	}

	public void setDomainModelObject(T domainModelObject) {
		this.domainModelObject = domainModelObject;
	}

	public void setMetadata(DomainModelDeltaMetadata metadata) {
		this.metadata = metadata;
	}

	public void setReplayEvents(Collection<DomainTransformEvent> replayEvents) {
		this.replayEvents = replayEvents;
	}

	public void setSignature(DomainModelDeltaSignature signature) {
		this.signature = signature;
	}

	public void setUnlinkedObjects(
			Collection<DomainModelDeltaEntity> unlinkedObjects) {
		this.unlinkedObjects = unlinkedObjects;
	}

	@Override
	public void unwrap(AsyncCallback<Void> completionCallback) {
		// already unwrapped
		completionCallback.onSuccess(null);
	}
}
