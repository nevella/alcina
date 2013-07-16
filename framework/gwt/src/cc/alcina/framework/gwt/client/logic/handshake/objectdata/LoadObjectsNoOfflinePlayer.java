package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.EndpointPlayer;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeState;

public class LoadObjectsNoOfflinePlayer extends RunnablePlayer<HandshakeState>
		implements ConsortPlayer {
	private LoadObjectsNoOfflineConsort loadObjectsConsort;

	public LoadObjectsNoOfflinePlayer() {
		addRequires(HandshakeState.LOADER_UI_INITIALISED);
		addRequires(HandshakeState.SERVICES_INITIALISED);
		addProvides(HandshakeState.OBJECT_DATA_LOADED);
		addProvides(HandshakeState.OBJECT_DATA_LOAD_FAILED);
		loadObjectsConsort = new LoadObjectsNoOfflineConsort();
	}

	public static class LoadObjectsNoOfflineConsort extends
			Consort<LoadObjectDataState> {
		public LoadObjectsNoOfflineConsort() {
			LoadObjectsHelloPlayer loadObjectsHelloPlayer = addPlayer(Registry
					.impl(LoadObjectsHelloPlayer.class));
			LoadObjectsFromRemotePlayer fromRemotePlayer = addPlayer(Registry
					.impl(LoadObjectsFromRemotePlayer.class));
			addPlayer(new EndpointPlayer(
					LoadObjectDataState.OBJECT_DATA_LOADED, null, true));
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