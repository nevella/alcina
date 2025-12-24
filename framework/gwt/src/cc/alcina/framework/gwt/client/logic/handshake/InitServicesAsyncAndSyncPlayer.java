package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.consort.Player.RunnablePlayer;

public class InitServicesAsyncAndSyncPlayer extends RunnablePlayer {
	public InitServicesAsyncAndSyncPlayer() {
		addRequires(HandshakeState.ASYNC_SERVICES_INITIALISED);
		addRequires(HandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
		addProvides(HandshakeState.SERVICES_INITIALISED);
	}

	@Override
	public void run() {
	}
}
