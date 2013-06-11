package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

public abstract class InitLoaderUiPlayer extends
		RunnablePlayer<HandshakeState> {
	public InitLoaderUiPlayer() {
		super();
		addRequires(HandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
		addProvides(HandshakeState.LOADER_UI_INITIALISED);
	}
}