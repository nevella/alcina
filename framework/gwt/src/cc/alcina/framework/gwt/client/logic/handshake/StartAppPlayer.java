package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.state.EndpointPlayer;

@Reflected
@Registration.Singleton
public class StartAppPlayer extends EndpointPlayer<HandshakeState> {
	public StartAppPlayer() {
		super(HandshakeState.MAIN_LAYOUT_INITIALISED);
	}

	@Override
	public void run() {
	}
}
