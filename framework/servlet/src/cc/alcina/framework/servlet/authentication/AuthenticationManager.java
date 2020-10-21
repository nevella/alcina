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
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.AuthenticationPersistence;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.servlet.AuthenticationTokenStore;

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

	public static final String COOKIE_NAME_IID = "IID";

	public static final String COOKIE_NAME_SESSIONID = "alcsessionid";

	public static AuthenticationManager get() {
		return Registry.impl(AuthenticationManager.class);
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
			context.session.setEndTime(new Date());
			context.session.setEndReason("Replaced by new session");
		}
		String sessionId = SEUtilities.generateId();
		AuthenticationSession session = persistence.createAuthenticationSession(
				context.iid, startDate, sessionId, user, authenticationType);
		context.session = session;
		context.tokenStore.setCookieValue(COOKIE_NAME_SESSIONID, sessionId);
		logger.info("Created session :: cookie: {} user: {} type: {}",
				sessionId, user, authenticationType);
		if (createClientInstance) {
			createClientInstance(context);
		}
		return session;
	}

	public ClientInstance createNonHttpClientInstance(String format,
			IUser user) {
		return null;
	}

	public Optional<ClientInstance> getContextClientInstance() {
		return Optional.ofNullable(ensureContext().clientInstance);
	}

	public Long getContextClientInstanceId() {
		return getContextClientInstance().map(ClientInstance::getId)
				.orElse(null);
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

	private void createClientInstance(AuthenticationContext context) {
		String userAgent = context.tokenStore.getUserAgent();
		context.clientInstance = persistence.createClientInstance(
				context.session, userAgent,
				context.tokenStore.getRemoteAddress(),
				context.tokenStore.getReferrer(), context.tokenStore.getUrl());
	}

	private void ensureAuthenticationSession(AuthenticationContext context) {
		String sessionId = context.tokenStore
				.getCookieValue(COOKIE_NAME_SESSIONID);
		sessionId = validateClientUid(sessionId);
		logger.trace("Ensure session: id {}", sessionId);
		if (Ax.notBlank(sessionId)) {
			context.session = persistence.getAuthenticationSession(sessionId);
		}
		boolean validSession = context.session != null
				&& context.session.getUser() != null
				&& !context.session.provideIsExpired();
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
			}
		}
		if (validSession) {
			Registry.impl(AuthenticationExpiration.class)
					.checkExpiration(context.session);
			logger.trace("Check expiration :: session {}", context.session);
			if (context.session.provideIsExpired()) {
				logger.info("Session expired :: session {}", context.session);
				context.session = null;
			}
		}
		if (!validSession) {
			createAuthenticationSession(new Date(),
					UserlandProvider.get().getAnonymousUser(), "anonymous",
					false);
			// FIXME - mvcc.5 - drop
			if (context.session.getIid().getRememberMeUser_id() != null) {
				persistence
						.populateSessionUserFromRememberMeUser(context.session);
			}
		} else {
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

	private void setupClientInstanceFromHeaders(AuthenticationContext context) {
		try {
			String headerId = context.tokenStore.getHeaderValue(
					AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY);
			headerId = validateClientUid(headerId);
			if (Ax.matches(headerId, "\\d+")) {
				ClientInstance instance = persistence
						.getClientInstance(Long.parseLong(headerId));
				if (instance != null) {
					if (instance.getAuthenticationSession() == null) {
						persistence.putSession(instance, context.session);
					}
					String headerAuth = context.tokenStore.getHeaderValue(
							AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY);
					if (Ax.matches(headerAuth, "\\d+")) {
						if (instance.getAuth().intValue() == Integer
								.parseInt(headerAuth)) {
							if (!instance.getAuthenticationSession()
									.provideIsExpired()) {
								context.clientInstance = instance;
							} else {
								context.tokenStore.addHeader(
										AlcinaRpcRequestBuilder.RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED,
										"true");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			context.clientInstance = null;
		}
	}

	private String validateClientUid(String uid) {
		return Ax.matches(uid, "server:.+") ? null : uid;
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
