package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

public class LoadObjectsHolder<DO extends DomainModelHolder> implements
		Serializable {
	private DO domainObjects;

	private List<DomainTransformEvent> replayEvents = new ArrayList<DomainTransformEvent>();

	public List<DomainTransformEvent> getReplayEvents() {
		return this.replayEvents;
	}

	public void setReplayEvents(List<DomainTransformEvent> replayEvents) {
		this.replayEvents = replayEvents;
	}
	private Long lastTransformId;

	private LoadObjectsRequest request;

	public DO getDomainObjects() {
		return domainObjects;
	}

	public Long getLastTransformId() {
		return lastTransformId;
	}
	public LoadObjectsRequest getRequest() {
		return request;
	}
	public void setDomainObjects(DO domainObjects) {
		this.domainObjects = domainObjects;
	}

	public void setLastTransformId(Long lastTransformId) {
		this.lastTransformId = lastTransformId;
	}
	public void setRequest(LoadObjectsRequest request) {
		this.request = request;
	}
}
