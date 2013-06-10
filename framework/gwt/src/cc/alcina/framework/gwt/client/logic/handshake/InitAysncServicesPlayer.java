package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeState;

public abstract class InitAysncServicesPlayer extends
		RunnablePlayer<ClientHandshakeState> {
	public InitAysncServicesPlayer() {
		super();
		addRequires(ClientHandshakeState.LOADER_UI_INITIALISED);
		addProvides(ClientHandshakeState.ASYNC_SERVICES_INITIALISED);
	}
}