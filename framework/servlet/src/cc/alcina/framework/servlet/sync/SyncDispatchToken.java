package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;


public class SyncDispatchToken<E extends SyncEndpointModel, I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	private E syncEndpointModel;

	private I interchangeModel;

	private D objectSetDeltaModel;

	public E getSyncEndpointModel() {
		return this.syncEndpointModel;
	}

	public void setSyncEndpointModel(E syncEndpointModel) {
		this.syncEndpointModel = syncEndpointModel;
	}

	public I getInterchangeModel() {
		return this.interchangeModel;
	}

	public void setInterchangeModel(I interchangeModel) {
		this.interchangeModel = interchangeModel;
	}

	public D getObjectSetDeltaModel() {
		return this.objectSetDeltaModel;
	}

	public void setObjectSetDeltaModel(D objectSetDeltaModel) {
		this.objectSetDeltaModel = objectSetDeltaModel;
	}
}
