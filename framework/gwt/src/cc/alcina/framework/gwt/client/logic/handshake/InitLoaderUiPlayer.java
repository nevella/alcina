package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

@ClientInstantiable
public abstract class InitLoaderUiPlayer
		extends RunnablePlayer<HandshakeState> {
	public InitLoaderUiPlayer() {
		super();
		addRequires(HandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
		addProvides(HandshakeState.LOADER_UI_INITIALISED);
	}
}