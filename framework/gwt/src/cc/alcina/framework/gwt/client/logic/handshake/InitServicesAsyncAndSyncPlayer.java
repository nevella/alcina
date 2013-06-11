package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

public class InitServicesAsyncAndSyncPlayer extends RunnablePlayer {
	public InitServicesAsyncAndSyncPlayer() {
		addRequires(HandshakeState.ASYNC_SERVICES_INITIALISED,
				HandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
		addProvides(HandshakeState.SERVICES_INITIALISED);
	}

	@Override
	public void run() {
		wasPlayed();
	}
}
