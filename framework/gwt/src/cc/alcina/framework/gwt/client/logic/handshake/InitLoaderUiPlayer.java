package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeState;

public abstract class InitLoaderUiPlayer extends
		RunnablePlayer<ClientHandshakeState> {
	public InitLoaderUiPlayer() {
		super();
		addRequires(ClientHandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
		addProvides(ClientHandshakeState.LOADER_UI_INITIALISED);
	}
}