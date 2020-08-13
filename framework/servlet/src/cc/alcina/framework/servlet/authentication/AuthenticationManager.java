package cc.alcina.framework.servlet.authentication;

import java.util.Date;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.AuthenticationPersistence;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.HttpContext;

@RegistryLocation(registryPoint = AuthenticationManager.class, implementationType = ImplementationType.SINGLETON)
public class AuthenticationManager {
	private static final String CONTEXT_AUTHENTICATION_CONTEXT = AuthenticationManager.class
			.getName() + ".CONTEXT_AUTHENTICATION_CONTEXT";

	private AuthenticationPersistence persistence;

	public AuthenticationManager() {
		this.persistence = AuthenticationPersistence.get();
	}

	public static AuthenticationManager get() {
		return Registry.impl(AuthenticationManager.class);
	}

	static class AuthenticationContext {
		Iid iid;

		ClientInstance clientInstance;

		AuthenticationSession session;

		String userName;

		HttpContext httpContext;

		IUser user;

		boolean authenticated;
	}

	public LoginResponse hello() {
		AuthenticationContext context = ensureContext();
		LoginResponse response = new LoginResponse();
		response.setOk(context.authenticated);
		createClientInstance(context);
		response.setClientInstance(context.clientInstance);
		return response;
	}

	private void createClientInstance(AuthenticationContext context) {
		String userAgent = CommonRemoteServiceServlet
				.getUserAgent(context.httpContext.request);
		persistence.createClientInstance(context.iid, userAgent,
				ServletLayerUtils
						.robustGetRemoteAddress(context.httpContext.request));
	}

	private void ensureIid(AuthenticationContext context) {
		// TODO - call me from was-session-helper
		// TODO Auto-generated method stub
	}

	private AuthenticationContext ensureContext() {
		return LooseContext.ensure(CONTEXT_AUTHENTICATION_CONTEXT,
				AuthenticationContext::new);
	}

	public AuthenticationSession createAuthenticationSession(Date startDate,
			IUser user, String authenticationType) {
		// note - will have iid from authenticationtoken
		// TODO Auto-generated method stub
		return persistence.createAuthenticationSession();
	}

	public void initialiseContext(HttpContext httpContext) {
		AuthenticationContext context = ensureContext();
		context.user = context.httpContext = httpContext;
		ensureIid(context);
		setupClientInstanceFromHeaders(context);
		checkClientInstanceValidity(context);
		if (context.authenticated) {
			context.user = context.clientInstance.getUser();
		}
	}

	private void checkClientInstanceValidity(AuthenticationContext context) {
		// TODO Auto-generated method stub
	}

	private void setupClientInstanceFromHeaders(AuthenticationContext context) {
		// TODO Auto-generated method stub
	}
}
