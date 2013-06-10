package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeState;

public class InitServicesAsyncAndSyncPlayer extends RunnablePlayer {
	public InitServicesAsyncAndSyncPlayer() {
		addRequires(ClientHandshakeState.ASYNC_SERVICES_INITIALISED,
				ClientHandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
		addProvides(ClientHandshakeState.SERVICES_INITIALISED);
	}

	@Override
	public void run() {
		wasPlayed();
	}
}
