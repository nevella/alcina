package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;

public class SyncDispatchToken<E extends SyncEndpointModel, I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	private E syncEndpointModel;

	private I interchangeModel;

	private D objectSetDeltaModel;

	public I getInterchangeModel() {
		return this.interchangeModel;
	}

	public D getObjectSetDeltaModel() {
		return this.objectSetDeltaModel;
	}

	public E getSyncEndpointModel() {
		return this.syncEndpointModel;
	}

	public void setInterchangeModel(I interchangeModel) {
		this.interchangeModel = interchangeModel;
	}

	public void setObjectSetDeltaModel(D objectSetDeltaModel) {
		this.objectSetDeltaModel = objectSetDeltaModel;
	}

	public void setSyncEndpointModel(E syncEndpointModel) {
		this.syncEndpointModel = syncEndpointModel;
	}
}
