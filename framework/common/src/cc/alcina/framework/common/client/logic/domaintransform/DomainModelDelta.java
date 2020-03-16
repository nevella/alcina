package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DomainModelDelta {
	public String getAppInstruction();

	public DomainModelHolder getDomainModelHolder();

	public DomainModelObject getDomainModelObject();

	public abstract DomainModelDeltaMetadata getMetadata();

	public Collection<DomainTransformEvent> getReplayEvents();

	public abstract DomainModelDeltaSignature getSignature();

	public Collection<DomainModelDeltaEntity> getUnlinkedObjects();

	public boolean hasLocalOnlyTransforms();

	void unwrap(AsyncCallback<Void> completionCallback);
}
