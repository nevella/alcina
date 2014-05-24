package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class DomainTranche<T extends DomainModelObject> implements
		DomainModelDelta, Serializable {
	private DomainModelHolder domainModelHolder;

	private Collection<HasIdAndLocalId> unlinkedObjects = new ArrayList<HasIdAndLocalId>();

	private Collection<DomainTransformEvent> replayEvents = new ArrayList<DomainTransformEvent>();

	private String appInstruction;

	private DomainModelDeltaSignature signature;

	private DomainModelDeltaMetadata metadata;

	private T domainModelObject;

	public String getAppInstruction() {
		return this.appInstruction;
	}

	public DomainModelHolder getDomainModelHolder() {
		return this.domainModelHolder;
	}

	@Override
	public DomainModelDeltaMetadata getMetadata() {
		return metadata;
	}

	public Collection<DomainTransformEvent> getReplayEvents() {
		return this.replayEvents;
	}

	@Override
	public DomainModelDeltaSignature getSignature() {
		return signature;
	}

	public Collection<HasIdAndLocalId> getUnlinkedObjects() {
		return this.unlinkedObjects;
	}

	public void setAppInstruction(String appInstruction) {
		this.appInstruction = appInstruction;
	}

	public void setDomainModelHolder(DomainModelHolder domainModelHolder) {
		this.domainModelHolder = domainModelHolder;
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

	public void setUnlinkedObjects(Collection<HasIdAndLocalId> unlinkedObjects) {
		this.unlinkedObjects = unlinkedObjects;
	}

	@Override
	public void unwrap(AsyncCallback<Void> completionCallback) {
		// already unwrapped
		completionCallback.onSuccess(null);
	}

	public void merge(DomainTranche other) {
		unlinkedObjects.addAll(other.getUnlinkedObjects());
		replayEvents.addAll(other.getReplayEvents());
		signature = other.signature;
	}

	@Override
	public boolean hasLocalOnlyTransforms() {
		return false;
	}

	public T getDomainModelObject() {
		return this.domainModelObject;
	}

	public void setDomainModelObject(T domainModelObject) {
		this.domainModelObject = domainModelObject;
	}
}
