package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.consort.Player.RunnablePlayer;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public abstract class SetupAfterObjectsPlayer extends RunnablePlayer {
	public SetupAfterObjectsPlayer() {
		addRequires(HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED);
		addProvides(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
	}
}