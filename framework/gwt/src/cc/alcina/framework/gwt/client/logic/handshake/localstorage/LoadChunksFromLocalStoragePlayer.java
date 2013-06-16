package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectDataState;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

public class LoadChunksFromLocalStoragePlayer extends
		RunnableAsyncCallbackPlayer<Void, LoadObjectDataState> implements
		ConsortPlayer {
	public LoadChunksFromLocalStoragePlayer() {
		addProvides(LoadObjectDataState.LOADED_CHUNKS_FROM_LOCAL_STORAGE);
		stateConsort = new LoadChunksFromLocalStorageConsort();
	}

	private LoadChunksFromLocalStorageConsort stateConsort;

	@Override
	public void run() {
		if ( !LocalTransformPersistence.isLocalStorageInstalled()) {
			wasPlayed();
			return;
		}
		new SubconsortSupport().run(consort, stateConsort, this);
	}

	@Override
	public Consort getStateConsort() {
		return stateConsort;
	}
}