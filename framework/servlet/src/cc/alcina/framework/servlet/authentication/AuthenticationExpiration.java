package cc.alcina.framework.servlet.authentication;

import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = AuthenticationExpiration.class, implementationType = ImplementationType.INSTANCE)
public class AuthenticationExpiration {
	public void checkExpiration(AuthenticationSession session) {
		// default to a no-op (no expiration)
	}
}
