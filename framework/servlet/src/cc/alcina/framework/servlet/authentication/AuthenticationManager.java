package cc.alcina.framework.servlet.authentication;

import java.util.Date;
import java.util.Objects;
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
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.servlet.AuthenticationTokenStore;

/**
 * This class handles authentication of each web request.
 *
 * Companion class Authenticator (subclassed per-app) handles password
 * validation, user creation - i.e. relatively infrequent authentication events
 */
@Registration.Singleton
public class AuthenticationManager {
	private static final String CONTEXT_AUTHENTICATION_CONTEXT = AuthenticationManager.class
			.getName() + ".CONTEXT_AUTHENTICATION_CONTEXT";

	public static final String CONTEXT_ALLOW_EXPIRED_ANONYMOUS_AUTHENTICATION_SESSION = AuthenticationManager.class
			.getName()
			+ ".CONTEXT_ALLOW_EXPIRED_ANONYMOUS_AUTHENTICATION_SESSION";

	public static final String COOKIE_NAME_IID = "IID";

	public static final String COOKIE_NAME_SESSIONID = "alcsessionid";

	public static AuthenticationManager get() {
		return Registry.impl(AuthenticationManager.class);
	}

	public static boolean hasContext() {
		return LooseContext.has(CONTEXT_AUTHENTICATION_CONTEXT);
	}

	public static Long provideAuthenticatedClientInstanceId() {
		return get().getContextClientInstance().map(ClientInstance::getId)
				.orElse(null);
	}

	private AuthenticationPersistence persistence;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public AuthenticationManager() {
		this.persistence = AuthenticationPersistence.get();
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
			logger.info(
					"Expired session :: id: {} reason: {} old_user: {} current_user: {}",
					context.session.getId(), context.session.getEndReason(),
					context.session.getUser(), user);
			invalidateSession(context.session, "Replaced by new session");
		}
		String sessionId = SEUtilities.generateId();
		AuthenticationSession session = persistence.createAuthenticationSession(
				context.iid, startDate, sessionId, user, authenticationType);
		context.session = session;
		context.tokenStore.setCookieValue(COOKIE_NAME_SESSIONID, sessionId);
		logger.info("Created session :: cookie: {} user: {} type: {}",
				sessionId, user, authenticationType);
		context.localAuthenticator.postCreateAuthenticationSession(session);
		if (createClientInstance) {
			createClientInstance(context);
		}
		return session;
	}

	public ClientInstance createNonHttpClientInstance(String format,
			IUser user) {
		return null;
	}

	public Optional<AuthenticationSession> getAuthenticationSession() {
		return Optional.ofNullable(ensureContext().session);
	}

	public Optional<ClientInstance> getContextClientInstance() {
		return Optional.ofNullable(ensureContext().clientInstance);
	}

	public Long getContextClientInstanceId() {
		return getContextClientInstance().map(ClientInstance::getId)
				.orElse(null);
	}

	public String getExternalAuthorizationUrl(Permission requiredPermission) {
		return ensureContext().localAuthenticator
				.getExternalAuthorizationUrl(requiredPermission);
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

	public void initialiseContext(AuthenticationTokenStore tokenStore) {
		AuthenticationContext context = ensureContext();
		context.tokenStore = tokenStore;
		IUser anonymousUser = UserlandProvider.get().getAnonymousUser();
		PermissionsManager.get().setUser(anonymousUser);
		PermissionsManager.get().setLoginState(LoginState.NOT_LOGGED_IN);
		ensureIid(context);
		ensureAuthenticationSession(context);
		setupClientInstanceFromHeaders(context);
		if (context.session != null
				&& context.session.getUser() != anonymousUser) {
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

	public void invalidateSession(AuthenticationSession session,
			String reason) {
		session.markInvalid(reason);
		ensureContext().localAuthenticator.invalidateSession(session);
	}

	private void createClientInstance(AuthenticationContext context) {
		String userAgent = context.tokenStore.getUserAgent();
		context.clientInstance = persistence.createClientInstance(
				context.session, userAgent,
				context.tokenStore.getRemoteAddress(),
				context.tokenStore.getReferrer(), context.tokenStore.getUrl());
		context.localAuthenticator
				.postCreateClientInstance(context.clientInstance);
	}

	private void ensureAuthenticationSession(AuthenticationContext context) {
		String sessionId = context.tokenStore
				.getCookieValue(COOKIE_NAME_SESSIONID);
		// note that this phase should use authSessionId reachable from the
		// context client instance, rather than the cookie sessionId, if there
		// is a context client instance
		AuthenticationSession fromUnvalidatedClientInstance = getUnvalidatedClientInstanceFromHeaders(
				context);
		if (fromUnvalidatedClientInstance != null) {
			sessionId = fromUnvalidatedClientInstance.getSessionId();
		}
		sessionId = validateClientUid(sessionId);
		logger.trace("Ensure session: id {}", sessionId);
		if (Ax.notBlank(sessionId)) {
			context.session = persistence.getAuthenticationSession(sessionId);
		}
		if (context.session != null && context.session.getMaxInstances() != 0
				&& fromUnvalidatedClientInstance == null) {
			if (context.session.getMaxInstances() <= context.session
					.getClientInstances().size()) {
				logger.info(
						"Ensure new session: (existing reached max instances): {}",
						sessionId);
				context.session = null;
			}
		}
		// a session is valid if expired *and* anonymous for a grace period
		// after expir
		boolean validSession = context.session != null
				&& context.session.getUser() != null;
		if (validSession && isExpired(context.session)) {
			// If a session is expired, there's probably a newer active for the
			// same IID. So it's important not to invalidate *that* by setting a
			// newer sesionId.
			//
			// The most common case will be an RPC call either
			// inflight before a login was processed, or post- (but still with
			// old client instance headers). In those cases, either throw
			// (default) or permit with the old session (method-specific
			// permission) if anonymous
			if (fromUnvalidatedClientInstance != null) {
				if (context.session.getUser().provideIsAnonymous()
						&& LooseContext.is(
								CONTEXT_ALLOW_EXPIRED_ANONYMOUS_AUTHENTICATION_SESSION)) {
					logger.warn(
							"Permitting expired session - anonymous/expired explicit permission - id: {}",
							sessionId);
					validSession = true;
				} else {
					// if this is an RPC request, we'll want to set up headers
					// informing the client of the reason for the exception.
					// so try to setup the client instance before throwing
					setupClientInstanceFromHeaders(context);
					logger.warn(
							"Throwing due to rpc exception with expired session id: {}",
							sessionId);
					throw new ExpiredClientInstanceException();
				}
			} else {
				// new servlet/webapp - create a new session
				validSession = false;
			}
		}
		if (validSession) {
			IUser sessionUser = context.session.getUser();
			boolean anonymousSession = Objects.equals(sessionUser.getUserName(),
					PermissionsManager.ANONYMOUS_USER_NAME);
			IUser anonymousUser = UserlandProvider.get().getAnonymousUser();
			if (anonymousSession && sessionUser != anonymousUser) {
				// handle differing anonymous user sessions (some authentication
				// providers have >1 'anonymous' users)
				context.session = createAuthenticationSession(new Date(),
						anonymousUser, "replace-anonymous", false);
			} else {
				Registry.impl(AuthenticationExpiration.class)
						.checkExpiration(context.session);
				logger.trace("Check expiration :: session {}", context.session);
				if (context.session.provideIsExpired()) {
					logger.info("Session expired :: session {}",
							context.session);
					context.session = null;
				}
			}
		} else {
			createAuthenticationSession(new Date(),
					UserlandProvider.get().getAnonymousUser(), "anonymous",
					false);
			// FIXME - mvcc.5 - drop
			if (context.session.getIid().getRememberMeUser_id() != null) {
				persistence
						.populateSessionUserFromRememberMeUser(context.session);
			}
		}
	}

	private AuthenticationContext ensureContext() {
		return LooseContext.ensure(CONTEXT_AUTHENTICATION_CONTEXT,
				AuthenticationContext::new);
	}

	private void ensureIid(AuthenticationContext context) {
		String instanceId = context.tokenStore.getCookieValue(COOKIE_NAME_IID);
		instanceId = validateClientUid(instanceId);
		if (Ax.notBlank(instanceId)) {
			context.iid = persistence.getIid(instanceId);
		}
		if (context.iid == null) {
			if (Ax.notBlank(instanceId)) {
				logger.warn("Invalid iid cookie :: {} {}", instanceId,
						context.tokenStore.getRemoteAddress());
			}
			instanceId = SEUtilities.generateId();
			context.tokenStore.setCookieValue(COOKIE_NAME_IID, instanceId);
			context.iid = persistence.createIid(instanceId);
		}
	}

	// although unvalidated, the header client-instance id itself must be
	// validated
	private AuthenticationSession getUnvalidatedClientInstanceFromHeaders(
			AuthenticationContext context) {
		try {
			String headerId = getValidatedHeaderId(context);
			headerId = validateClientUid(headerId);
			if (Ax.matches(headerId, "\\d+")) {
				ClientInstance instance = persistence
						.getClientInstance(Long.parseLong(headerId));
				if (instance != null) {
					AuthenticationSession session = instance
							.getAuthenticationSession();
					if (session != null) {
						return session;
					}
				}
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean isExpired(AuthenticationSession session) {
		if (!Configuration.is("sessionExpirationEnabled")) {
			return false;
		}
		ensureContext().localAuthenticator.checkExternalExpiration(session);
		boolean result = session.provideIsExpired();
		if (result && session.getEndTime() == null) {
			logger.warn(
					"Marking authentication session as ended (login disabled?) - {} {}",
					session, session.getUser());
			session.setEndTime(new Date());
			session.setEndReason("Access not permitted");
		}
		return result;
	}

	private void setupClientInstanceFromHeaders(AuthenticationContext context) {
		try {
			String headerId = getValidatedHeaderId(context);
			headerId = validateClientUid(headerId);
			if (Ax.matches(headerId, "\\d+")) {
				ClientInstance instance = persistence
						.getClientInstance(Long.parseLong(headerId));
				if (instance != null) {
					AuthenticationSession session = instance
							.getAuthenticationSession();
					if (session == null) {
						persistence.putSession(instance, context.session);
					}
					if (!isExpired(session)) {
						context.clientInstance = instance;
					} else {
						context.tokenStore.addHeader(
								AlcinaRpcRequestBuilder.RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED,
								headerId);
						logger.warn(
								"Sending client instance expired:  - {} {} {}",
								instance, session, session.getUser());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			context.clientInstance = null;
		}
	}

	/*
	 * Disallows a server-side 'session cookie'
	 */
	private String validateClientUid(String uid) {
		return Ax.matches(uid, "server:.+") ? null : uid;
	}

	String getValidatedHeaderId(AuthenticationContext context) {
		String headerId = context.tokenStore.getHeaderValue(
				AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY);
		String headerAuth = context.tokenStore.getHeaderValue(
				AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY);
		if (Ax.matches(headerId, "\\d+") && Ax.matches(headerAuth, "\\d+")) {
			ClientInstance instance = persistence
					.getClientInstance(Long.parseLong(headerId));
			if (instance != null) {
				if (instance.getAuth().intValue() == Integer
						.parseInt(headerAuth)) {
					return headerId;
				}
			}
		}
		return null;
	}

	public static class ExpiredClientInstanceException
			extends RuntimeException {
		public ExpiredClientInstanceException() {
			super("Not authorized - client instance expired");
		}
	}

	static class AuthenticationContext {
		Iid iid;

		ClientInstance clientInstance;

		AuthenticationSession session;

		String userName;

		AuthenticationTokenStore tokenStore;

		private Authenticator<?> localAuthenticator = Registry
				.impl(Authenticator.class);

		<U extends Entity & IUser> Authenticator<U> typedAuthenticator() {
			return (Authenticator<U>) localAuthenticator;
		}
	}
}
