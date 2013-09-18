package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeState;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectDataState;
import cc.alcina.framework.gwt.persistence.client.DeltaStore;

public class MergeObjectDeltasPlayer extends
		RunnableAsyncCallbackPlayer<Void, LoadObjectDataState> {
	

	public MergeObjectDeltasPlayer() {
		addRequires(LoadObjectDataState.OBJECT_DATA_LOADED);
		addProvides(LoadObjectDataState.DELTA_STORE_MERGED_IF_NECESSARY);
	}

	@Override
	public void run() {
		DeltaStore.get().mergeResponse(
				HandshakeConsortModel.get().getLoadObjectsResponse(), this);
	}
}