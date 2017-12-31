package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.EndpointPlayer;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeState;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.MergeObjectDeltasPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.RetrieveLocalModelTransformDeltasPlayer;

public class LoadObjectsPlayer extends RunnablePlayer<HandshakeState>
		implements ConsortPlayer {
	private LoadObjectsConsort loadObjectsConsort;

	public LoadObjectsPlayer() {
		addRequires(HandshakeState.LOADER_UI_INITIALISED);
		addRequires(HandshakeState.SERVICES_INITIALISED);
		addProvides(HandshakeState.OBJECT_DATA_LOADED);
		addProvides(HandshakeState.OBJECT_DATA_LOAD_FAILED);
		loadObjectsConsort = new LoadObjectsConsort();
	}

	@Override
	public Consort getStateConsort() {
		return loadObjectsConsort;
	}

	@Override
	public void run() {
		new SubconsortSupport().run(consort, loadObjectsConsort, this);
	}

	@Override
	protected void wasPlayed() {
		boolean success = loadObjectsConsort
				.containsState(LoadObjectDataState.OBJECT_DATA_LOADED);
		super.wasPlayed(success ? HandshakeState.OBJECT_DATA_LOADED
				: HandshakeState.OBJECT_DATA_LOAD_FAILED);
	}

	public static class LoadObjectsConsort
			extends Consort<LoadObjectDataState> {
		public LoadObjectsConsort() {
			addPlayer(Registry.impl(LoadObjectsHelloPlayer.class));
			addPlayer(Registry.impl(LoadObjectsFromRemotePlayer.class));
			addPlayer(new RetrieveLocalModelTransformDeltasPlayer());
			addPlayer(Registry.impl(CheckSoleOfflineTabPlayer.class));
			addPlayer(Registry.impl(CheckOfflineSufficentPlayer.class));
			addPlayer(new MergeObjectDeltasPlayer());
			addPlayer(new EndpointPlayer(
					LoadObjectDataState.DELTA_STORE_MERGED_IF_NECESSARY, null,
					true));
			addPlayer(new EndpointPlayer(
					LoadObjectDataState.OBJECT_DATA_LOAD_FAILED, null, true));
		}

		@Override
		public void finished() {
			Registry.impl(HandshakeConsortModel.class)
					.clearLoadObjectsNotifier();
			super.finished();
		}
	}
}