package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeState;

public class StartAppPlayer extends RunnablePlayer {
	public StartAppPlayer() {
		addRequires(ClientHandshakeState.MAIN_LAYOUT_INITIALISED);
	}

	@Override
	public void run() {
		wasPlayed();
	}
}