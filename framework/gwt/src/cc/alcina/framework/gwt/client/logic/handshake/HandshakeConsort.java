package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeState;

@RegistryLocation(registryPoint = HandshakeConsort.class, implementationType = ImplementationType.SINGLETON)
public class HandshakeConsort extends Consort<ClientHandshakeState> {
	
	public HandshakeConsort() {
	}
	public boolean isAfterDomainModelLoaded() {
		return containsState(ClientHandshakeState.SETUP_AFTER_OBJECTS_LOADED);
		
	}
	
}