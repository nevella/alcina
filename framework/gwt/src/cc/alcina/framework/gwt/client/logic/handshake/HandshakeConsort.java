package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;

/**
 * <h3>Notes:</h3>
 * <ul>
 * <li>If you use local persistence, make sure to call <br>
 * <code>new SaveToLocalStorageConsort().start();</code><br>
 * at some point
 * </ul>
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = HandshakeConsort.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class HandshakeConsort extends Consort<HandshakeState, HandshakeSignal> {
	public HandshakeConsort() {
	}

	public boolean isAfterDomainModelLoaded() {
		return containsState(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
	}

	public void logout() {
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(null);
		signal(HandshakeSignal.LOGGED_OUT);
	}

	public void handleLoggedIn(LoginResponse loginResponse) {
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(
				loginResponse);
		signal(HandshakeSignal.LOGGED_IN);
	}
}