package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DomainModelDelta {
	public DomainModelHolder getDomainModelHolder();

	public DomainModelObject getDomainModelObject();

	public Collection<HasIdAndLocalId> getUnlinkedObjects();

	public Collection<DomainTransformEvent> getReplayEvents();

	public String getAppInstruction();

	void unwrap(AsyncCallback<Void> completionCallback);

	public abstract DomainModelDeltaSignature getSignature();

	public abstract DomainModelDeltaMetadata getMetadata();

	public boolean hasLocalOnlyTransforms();
}
