package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.EndpointPlayer;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeState;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.LoadChunksFromLocalStoragePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.RetrieveLocalModelTransformDeltasPlayer;

public class LoadObjectsPlayer extends RunnablePlayer<HandshakeState> implements
		ConsortPlayer {
	private LoadObjectsConsort loadObjectsConsort;

	public LoadObjectsPlayer() {
		addRequires(HandshakeState.LOADER_UI_INITIALISED,
				HandshakeState.SERVICES_INITIALISED);
		addProvides(HandshakeState.OBJECT_DATA_LOADED,
				HandshakeState.OBJECT_DATA_LOAD_FAILED);
		loadObjectsConsort = new LoadObjectsConsort();
	}

	public static class LoadObjectsConsort extends
			Consort<LoadObjectDataState> {
		public LoadObjectsConsort() {
			LoadObjectsHelloPlayer loadObjectsHelloPlayer = addPlayer(Registry
					.impl(LoadObjectsHelloPlayer.class));
			LoadObjectsFromRemotePlayer fromRemotePlayer = addPlayer(Registry
					.impl(LoadObjectsFromRemotePlayer.class));
			addPlayer(new LoadChunksFromLocalStoragePlayer());
			loadObjectsHelloPlayer
					.addRequires(LoadObjectDataState.LOADED_CHUNKS_FROM_LOCAL_STORAGE);
			addPlayer(new RetrieveLocalModelTransformDeltasPlayer());
			addPlayer(Registry.impl(CheckSoleOfflineTabPlayer.class));
			addPlayer(new CheckOfflineSufficentPlayer());
			addPlayer(new EndpointPlayer(
					LoadObjectDataState.OBJECT_DATA_LOADED, null));
			addPlayer(new EndpointPlayer(
					LoadObjectDataState.OBJECT_DATA_LOAD_FAILED, null));
		}

		@Override
		public void finished() {
			Registry.impl(HandshakeConsortModel.class)
					.clearLoadObjectsNotifier();
			super.finished();
		}
	}

	@Override
	protected void wasPlayed() {
		boolean success = loadObjectsConsort
				.containsState(LoadObjectDataState.OBJECT_DATA_LOADED);
		super.wasPlayed(success ? HandshakeState.OBJECT_DATA_LOADED
				: HandshakeState.OBJECT_DATA_LOAD_FAILED);
	}

	@Override
	public void run() {
		new SubconsortSupport().run(consort, loadObjectsConsort, this);
	}

	@Override
	public Consort getStateConsort() {
		return loadObjectsConsort;
	}
}