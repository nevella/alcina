package cc.alcina.framework.servlet.authentication;

import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.Iid;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.AuthenticationPersistence;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.HttpContext;

/**
 * This class handles authentication of each web request.
 * 
 * Companion class Authenticator (subclassed per-app) handles password
 * validation, user creation - i.e. relatively infrequent authentication events
 */
@RegistryLocation(registryPoint = AuthenticationManager.class, implementationType = ImplementationType.SINGLETON)
public class AuthenticationManager {
	private static final String CONTEXT_AUTHENTICATION_CONTEXT = AuthenticationManager.class
			.getName() + ".CONTEXT_AUTHENTICATION_CONTEXT";

	private AuthenticationPersistence persistence;

	public static final String COOKIE_NAME_IID = "IID";

	public static final String COOKIE_NAME_SESSIONID = "alcsessionid";

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

		private Authenticator<?> localAuthenticator = Registry
				.impl(Authenticator.class);

		<U extends Entity & IUser> Authenticator<U> typedAuthenticator() {
			return (Authenticator<U>) localAuthenticator;
		}
	}

	public LoginResponse hello() {
		AuthenticationContext context = ensureContext();
		LoginResponse response = new LoginResponse();
		response.setOk(true);
		createClientInstance(context);
		Transaction.commit();
		response.setClientInstance(context.clientInstance);
		response.setUser(
				context.clientInstance.getAuthenticationSession().getUser());
		return response;
	}

	private void createClientInstance(AuthenticationContext context) {
		String userAgent = CommonRemoteServiceServlet
				.getUserAgent(context.httpContext.request);
		context.clientInstance = persistence.createClientInstance(
				context.session, userAgent, ServletLayerUtils
						.robustGetRemoteAddress(context.httpContext.request));
	}

	private void ensureIid(AuthenticationContext context) {
		String instanceId = context.httpContext.getCookieValue(COOKIE_NAME_IID);
		instanceId = validateClientUid(instanceId);
		if (Ax.notBlank(instanceId)) {
			context.iid = persistence.getIid(instanceId);
		}
		if (context.iid == null) {
			instanceId = SEUtilities.generateId();
			context.httpContext.setCookieValue(COOKIE_NAME_IID, instanceId);
			context.iid = persistence.createIid(instanceId);
		}
	}

	private AuthenticationContext ensureContext() {
		return LooseContext.ensure(CONTEXT_AUTHENTICATION_CONTEXT,
				AuthenticationContext::new);
	}

	/**
	 * 'createClientInstance' is used when the request needs a new
	 * clientInstance immediately - basically any time except during the
	 * start-of-request authentication setup
	 */
	public AuthenticationSession createAuthenticationSession(Date startDate,
			IUser user, String authenticationType,
			boolean createClientInstance) {
		AuthenticationContext context = ensureContext();
		if (context.session != null) {
			context.session.setEndTime(new Date());
			context.session.setEndReason("Replaced by new session");
		}
		String sessionId = SEUtilities.generateId();
		AuthenticationSession session = persistence.createAuthenticationSession(
				context.iid, startDate, sessionId, user, authenticationType);
		context.session = session;
		context.httpContext.setCookieValue(COOKIE_NAME_SESSIONID, sessionId);
		logger.warn("Created session :: cookie: {} user: {} type: {}",
				sessionId, user, authenticationType);
		if (createClientInstance) {
			createClientInstance(context);
		}
		return session;
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	public void initialiseContext(HttpContext httpContext) {
		AuthenticationContext context = ensureContext();
		IUser anonymousUser = UserlandProvider.get().getAnonymousUser();
		PermissionsManager.get().setUser(anonymousUser);
		PermissionsManager.get().setLoginState(LoginState.NOT_LOGGED_IN);
		context.httpContext = httpContext;
		ensureIid(context);
		ensureAuthenticationSession(context);
		setupClientInstanceFromHeaders(context);
		if (context.session.getUser() != anonymousUser) {
			PermissionsManager.get().setUser(context.session.getUser());
			PermissionsManager.get().setLoginState(LoginState.LOGGED_IN);
		}
		if (context.clientInstance != null) {
			persistence.wasAccessed(context.clientInstance);
			PermissionsManager.get().setClientInstance(context.clientInstance);
		}
		// all auth objects persisted as root
		Transaction.commit();
	}

	private void ensureAuthenticationSession(AuthenticationContext context) {
		String sessionId = context.httpContext
				.getCookieValue(COOKIE_NAME_SESSIONID);
		sessionId = validateClientUid(sessionId);
		logger.warn("Ensure session: id {}", sessionId);
		if (Ax.notBlank(sessionId)) {
			context.session = persistence.getAuthenticationSession(sessionId);
		}
		if (context.session != null) {
			Registry.impl(AuthenticationExpiration.class)
					.checkExpiration(context.session);
			logger.warn("Check expiration :: session {}", context.session);
			if (context.session.provideIsExpired()) {
				logger.warn("Session expired :: session {}", context.session);
				context.session = null;
			}
		}
		if (context.session == null) {
			createAuthenticationSession(new Date(),
					UserlandProvider.get().getAnonymousUser(), "anonymous",
					false);
		}
	}

	private String validateClientUid(String uid) {
		return Ax.matches(uid, "server:.+") ? null : uid;
	}

	private void setupClientInstanceFromHeaders(AuthenticationContext context) {
		try {
			String headerId = context.httpContext.request.getHeader(
					AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY);
			headerId = validateClientUid(headerId);
			if (Ax.matches(headerId, "\\d+")) {
				ClientInstance instance = persistence
						.getClientInstance(Long.parseLong(headerId));
				if (instance.getAuthenticationSession() == null) {
					persistence.putSession(instance, context.session);
				}
				String headerAuth = context.httpContext.request.getHeader(
						AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY);
				if (Ax.matches(headerAuth, "\\d+")) {
					if (instance.getAuth().intValue() == Integer
							.parseInt(headerAuth)) {
						if (!instance.getAuthenticationSession()
								.provideIsExpired()) {
							context.clientInstance = instance;
						} else {
							context.httpContext.response.addHeader(
									AlcinaRpcRequestBuilder.RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED,
									"true");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			context.clientInstance = null;
		}
	}

	public Optional<ClientInstance> getContextClientInstance() {
		return Optional.ofNullable(ensureContext().clientInstance);
	}

	public static Long provideAuthenticatedClientInstanceId() {
		return get().getContextClientInstance().map(ClientInstance::getId)
				.orElse(null);
	}
}
