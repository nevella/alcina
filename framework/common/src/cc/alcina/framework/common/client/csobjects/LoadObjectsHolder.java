package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.RepeatingCommandWithPostCompletionCallback;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;

public class LoadObjectsHolder<DO extends DomainModelHolder> implements
		Serializable, DomainModelDelta {
	private DO domainModelHolder;

	private List<DomainTransformEvent> replayEvents = new ArrayList<DomainTransformEvent>();

	private List<HasIdAndLocalId> unlinkedObjects = new ArrayList<HasIdAndLocalId>();

	private Long lastTransformId;

	private LoadObjectsRequest request;

	private String appInstruction;
	
	public String getAppInstruction() {
		return this.appInstruction;
	}

	public DO getDomainModelHolder() {
		return domainModelHolder;
	}

	@Override
	public Long getLastTransformId() {
		return lastTransformId;
	}

	public List<DomainTransformEvent> getReplayEvents() {
		return this.replayEvents;
	}

	public LoadObjectsRequest getRequest() {
		return request;
	}

	public List<HasIdAndLocalId> getUnlinkedObjects() {
		return this.unlinkedObjects;
	}

	public void setAppInstruction(String appInstruction) {
		this.appInstruction = appInstruction;
	}

	public void setDomainModelHolder(DO domainModelHolder) {
		this.domainModelHolder = domainModelHolder;
	}

	public void setLastTransformId(Long lastTransformId) {
		this.lastTransformId = lastTransformId;
	}

	public void setReplayEvents(List<DomainTransformEvent> replayEvents) {
		this.replayEvents = replayEvents;
	}

	public void setRequest(LoadObjectsRequest request) {
		this.request = request;
	}

	public void setUnlinkedObjects(List<HasIdAndLocalId> unlinkedObjects) {
		this.unlinkedObjects = unlinkedObjects;
	}

	@Override
	public DomainTransformRequest getUncomittedDomainTransformRequest() {
		return null;
	}

	@Override
	public Integer getDomainTransformRequestReplayId() {
		return null;
	}
	@Override
	public void unwrap(AsyncCallback<Void> completionCallback) {
		completionCallback.onSuccess(null);
	}

	public String getTypeSignature() {
		return getRequest().getTypeSignature();
	}

}
