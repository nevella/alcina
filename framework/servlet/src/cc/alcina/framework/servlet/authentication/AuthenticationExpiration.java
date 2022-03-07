package cc.alcina.framework.servlet.authentication;

import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration(AuthenticationExpiration.class)
public class AuthenticationExpiration {
	public void checkExpiration(AuthenticationSession session) {
		// default to a no-op (no expiration)
	}
}
