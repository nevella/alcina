package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

@ClientInstantiable
public abstract class InitAysncServicesPlayer extends
		RunnablePlayer<HandshakeState> {
	public InitAysncServicesPlayer() {
		super();
		addRequires(HandshakeState.LOADER_UI_INITIALISED);
		addProvides(HandshakeState.ASYNC_SERVICES_INITIALISED);
	}
}