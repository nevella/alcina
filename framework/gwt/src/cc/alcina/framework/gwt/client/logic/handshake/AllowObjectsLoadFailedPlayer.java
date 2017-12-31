package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

public class AllowObjectsLoadFailedPlayer extends RunnablePlayer {
	public AllowObjectsLoadFailedPlayer() {
		addRequires(HandshakeState.OBJECT_DATA_LOAD_FAILED);
		addProvides(HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED);
	}

	@Override
	public void run() {
		// joinpoint, do nothing
	}
}