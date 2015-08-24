package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.state.EndpointPlayer;
@ClientInstantiable
@RegistryLocation(registryPoint=StartAppPlayer.class,implementationType=ImplementationType.SINGLETON)
public class StartAppPlayer  extends EndpointPlayer<HandshakeState> {
	public StartAppPlayer() {
		super(HandshakeState.MAIN_LAYOUT_INITIALISED);
	}

	@Override
	public void run() {
	}
}