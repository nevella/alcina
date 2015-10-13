package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.sync.SyncInterchangeModel;

public abstract class SyncDispatcher<E extends SyncEndpointModel, I extends SyncInterchangeModel, D extends SyncDeltaModel> {
	protected GraphTransformer<E, I> transformer;

	public SyncDispatchToken<E, I, D> syncDispatchToken = new SyncDispatchToken();

	protected MergeRequestDispatcher<I, D> mergeRequestDispatcher;

	protected FlatDeltaPersister<D> deltaApplicator;

	protected void prepareInterchangeModel() {
		syncDispatchToken.setInterchangeModel(transformer
				.transform(syncDispatchToken.getSyncEndpointModel()));
	}

	protected void getDelta() throws Exception {
		syncDispatchToken.setObjectSetDeltaModel(mergeRequestDispatcher
				.dispatch(syncDispatchToken.getInterchangeModel()));
	}

	protected void applyDelta() throws Exception {
		deltaApplicator.apply(syncDispatchToken.getObjectSetDeltaModel());
	}

	public void setInitialEndpointModel(E endpointModel) {
		syncDispatchToken.setSyncEndpointModel(endpointModel);
	}

	public void run() throws Exception {
		prepareInterchangeModel();
		getDelta();
		applyDelta();
	}
}
