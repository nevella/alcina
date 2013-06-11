package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

public class AllowObjectsLoadFailedPlayer extends RunnablePlayer {
	public AllowObjectsLoadFailedPlayer() {
		addRequires(HandshakeState.OBJECTS_LOAD_FAILED);
		addProvides(HandshakeState.OBJECTS_REGISTERED);
	}

	@Override
	public void run() {
		wasPlayed();
	}
}