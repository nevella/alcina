package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.consort.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.ClientConfiguration;

public class InitSynchronousServicesPlayer extends RunnablePlayer {
	private ClientConfiguration clientConfiguration;

	public InitSynchronousServicesPlayer(
			ClientConfiguration clientConfiguration) {
		this.clientConfiguration = clientConfiguration;
		addProvides(HandshakeState.SYNCHRONOUS_SERVICES_INITIALISED);
	}

	@Override
	public void run() {
		clientConfiguration.initServices();
	}
}