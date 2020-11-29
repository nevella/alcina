package cc.alcina.framework.entity.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.util.MethodContext;

@RegistryLocation(registryPoint = AuthenticationPersistence.class, implementationType = ImplementationType.SINGLETON)
public class AuthenticationPersistence {
	public static final transient String CONTEXT_IDLE_TIMEOUT_DISABLED = AuthenticationPersistence.class
			.getName() + ".CONTEXT_IDLE_TIMEOUT_DISABLED";

	public static AuthenticationPersistence get() {
		return Registry.impl(AuthenticationPersistence.class);
	}

	@SuppressWarnings("unused")
	private Map<String, String> lookupQueries = new ConcurrentHashMap<>();

	private Logger logger = LoggerFactory.getLogger(getClass());

	public AuthenticationSession createAuthenticationSession(Iid iid,
			Date startDate, String sessionId, IUser user,
			String authenticationType) {
		Class<? extends AuthenticationSession> clazz = AlcinaPersistentEntityImpl
				.getImplementation(AuthenticationSession.class);
		AuthenticationSession session = Domain.create(clazz);
		if (iid.getVersionNumber() == -1) {
			iid.setVersionNumber(0);
			String eql = Ax.format("update iid set optlock=0 where id=%s",
					iid.getId());
			callWithEntityManager(em -> {
				em.createNativeQuery(eql).executeUpdate();
				//
				return null;
			});
		}
		session.setIid(iid);
		session.setSessionId(sessionId);
		session.setUser(user);
		session.setAuthenticationType(authenticationType);
		session.setStartTime(startDate);
		return session;
	}

	public void createBootstrapClientInstance() {
		BootstrapCreationResult bootstrapInstanceResult = callWithEntityManager(
				new BootstrapInstanceCreator());
		ClientInstance persistent = bootstrapInstanceResult.clientInstance;
		EntityLayerObjects.get().setServerAsClientInstance(persistent);
		ClientInstance preCommit = Domain.find(persistent);
		EntityLayerObjects.get().setServerAsClientInstance(preCommit);
		// publish as transforms (repeating the direct ejb create) to (a)
		// force consistency in local domain and (b) allow for replay
		bootstrapInstanceResult.createdDetached.stream()
				.forEach(e -> TransformManager.get()
						.objectsToDtes(
								Collections.singletonList(Domain.find(e)),
								e.entityClass(), false)
						.forEach(TransformManager.get()::addTransform));
		Transactions.getEnqueuedLazyLoads().clear();
		MethodContext.instance().withContextTrue(
				TransformPersisterInPersistenceContext.CONTEXT_REPLAYING_FOR_LOGS)
				.run(() -> Transaction.commit());
		ClientInstance domainVersion = Domain.find(persistent);
		// ensure this instance is the only one ever created in the domain
		EntityLayerObjects.get().setServerAsClientInstance(domainVersion);
	}

	public ClientInstance createClientInstance(AuthenticationSession session,
			String userAgent, String remoteAddress, String referrer,
			String url) {
		Class<? extends ClientInstance> clazz = AlcinaPersistentEntityImpl
				.getImplementation(ClientInstance.class);
		ClientInstance clientInstance = Domain.create(clazz);
		clientInstance.setAuthenticationSession(session);
		clientInstance.setUser_id(session.getUser().getId());
		clientInstance.setHelloDate(new Date());
		clientInstance.setUserAgent(userAgent);
		clientInstance.setAuth(Math.abs(new Random().nextInt()));
		clientInstance.setIpAddress(remoteAddress);
		clientInstance
				.setReferrer(CommonUtils.trimToWsChars(referrer, 240, true));
		clientInstance.setUrl(CommonUtils.trimToWsChars(url, 240, true));
		clientInstance
				.setBotUserAgent(EntityLayerUtils.isBotUserAgent(userAgent));
		return clientInstance;
	}

	public Iid createIid(String instanceId) {
		Class<? extends Iid> clazz = AlcinaPersistentEntityImpl
				.getImplementation(Iid.class);
		Iid iid = Domain.create(clazz);
		iid.setInstanceId(instanceId);
		return iid;
	}

	public AuthenticationSession getAuthenticationSession(String sessionId) {
		Class<? extends AuthenticationSession> clazz = AlcinaPersistentEntityImpl
				.getImplementation(AuthenticationSession.class);
		Optional<? extends AuthenticationSession> domainEntity = Domain
				.query(clazz).filter("sessionId", sessionId).optional();
		if (domainEntity.isPresent()) {
			return domainEntity.get();
		}
		String query = Ax.format(
				"select authenticationSession.id from %s authenticationSession where %s='%s'",
				clazz.getSimpleName(), "sessionId", sessionId);
		/*
		 * caching issue if not in postProcess - see DomainStore.find
		 */
		// if (lookupQueries.put(query, query) != null) {
		// return null;
		// }
		Long id = Ax.first((List<Long>) callWithEntityManager(
				em -> em.createQuery(query).getResultList()));
		if (id == null) {
			return null;
		}
		AuthenticationSession session = Domain.find(clazz, id);
		return session;
	}

	public ClientInstance getClientInstance(long clientInstanceId) {
		return domainImpl(ClientInstance.class, clientInstanceId);
	}

	public Iid getIid(String instanceId) {
		Class<? extends Iid> clazz = AlcinaPersistentEntityImpl
				.getImplementation(Iid.class);
		Optional<? extends Iid> domainEntity = Domain.query(clazz)
				.filter("instanceId", instanceId).optional();
		if (domainEntity.isPresent()) {
			return domainEntity.get();
		}
		String query = Ax.format("select id from %s where %s='%s'",
				clazz.getSimpleName(), "instanceId", instanceId);
		/*
		 * caching issue if not in postProcess - see DomainStore.find
		 */
		// if (lookupQueries.put(query, query) != null) {
		// return null;
		// }
		Long id = Ax.first((List<Long>) callWithEntityManager(
				em -> em.createQuery(query).getResultList()));
		return id == null ? null : Domain.find(clazz, id);
	}

	public void populateSessionUserFromRememberMeUser(
			AuthenticationSession session) {
		Iid iid = session.getIid();
		Long rememberMeUser_id = iid.getRememberMeUser_id();
		session.setUser(UserlandProvider.get().getUserById(rememberMeUser_id));
		session.setAuthenticationType("legacy-iid");
		logger.warn("Nulling iid rememberMeUser_id :: {} - {}", iid.getId(),
				rememberMeUser_id);
		iid.setRememberMeUser_id(null);
		Transaction.commit();
	}

	public void putSession(ClientInstance instance,
			AuthenticationSession session) {
		instance.setAuthenticationSession(session);
		Transaction.commit();
	}

	public boolean validateClientInstance(long clientInstanceId,
			int clientInstanceAuth) {
		return Optional.ofNullable(getClientInstance(clientInstanceId))
				.map(ci -> ci.getAuth() == clientInstanceAuth).orElse(false);
	}

	public void wasAccessed(ClientInstance clientInstance) {
		TransformCommit.get().enqueueBackendTransform(() -> {
			clientInstance.setLastAccessed(new Date());
			clientInstance.getAuthenticationSession()
					.setLastAccessed(new Date());
		});
	}

	private <V> V callWithEntityManager(Function<EntityManager, V> function) {
		return CommonPersistenceProvider.get().getCommonPersistence()
				.callWithEntityManager(function);
	}

	private BootstrapCreationResult
			createBootstrapClientInstance(EntityManager em) {
		BootstrapCreationResult result = new BootstrapCreationResult();
		String hostName = EntityLayerUtils.getLocalHostName();
		String authenticationSessionUid = Ax.format("servlet:%s", hostName);
		String iidUid = authenticationSessionUid;
		List<Entity> createdObjects = new ArrayList<>();
		Iid iid = (Iid) Ax.first(em.createQuery(Ax.format(
				"select iid from %s iid where instanceId = '%s'",
				AlcinaPersistentEntityImpl
						.getImplementationSimpleClassName(Iid.class),
				iidUid)).getResultList());
		if (iid == null) {
			iid = AlcinaPersistentEntityImpl
					.getNewImplementationInstance(Iid.class);
			iid.setInstanceId(iidUid);
			em.persist(iid);
			createdObjects.add(iid);
		}
		AuthenticationSession authenticationSession = (AuthenticationSession) Ax
				.first(em.createQuery(Ax.format(
						"select authenticationSession from %s authenticationSession where sessionId = '%s'",
						AlcinaPersistentEntityImpl
								.getImplementationSimpleClassName(
										AuthenticationSession.class),
						authenticationSessionUid)).getResultList());
		if (authenticationSession == null) {
			authenticationSession = AlcinaPersistentEntityImpl
					.getNewImplementationInstance(AuthenticationSession.class);
			authenticationSession.setSessionId(authenticationSessionUid);
			authenticationSession.setStartTime(new Date());
			authenticationSession.setUser((IUser) persistentImpl(em,
					(Entity) PermissionsManager.get().getUser()));
			authenticationSession.setAuthenticationType("server-instance");
			em.persist(authenticationSession);
			createdObjects.add(authenticationSession);
		}
		ClientInstance clientInstance = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(ClientInstance.class);
		clientInstance.setHelloDate(new Date());
		clientInstance.setUserAgent(authenticationSessionUid);
		clientInstance.setAuthenticationSession(authenticationSession);
		clientInstance.setAuth(Math.abs(new Random().nextInt()));
		clientInstance.setIpAddress("127.0.0.1");
		clientInstance.setBotUserAgent(false);
		em.persist(clientInstance);
		Iid detachedIid = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(Iid.class);
		detachedIid.setId(iid.getId());
		if (createdObjects.contains(iid)) {
			result.createdDetached.add(detachedIid);
		}
		AuthenticationSession detachedSession = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(AuthenticationSession.class);
		detachedSession.setId(authenticationSession.getId());
		detachedSession.setIid(detachedIid);
		if (createdObjects.contains(authenticationSession)) {
			result.createdDetached.add(detachedSession);
		}
		ClientInstance detachedInstance = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(ClientInstance.class);
		detachedInstance.setId(clientInstance.getId());
		detachedInstance.setAuthenticationSession(detachedSession);
		result.createdDetached.add(detachedInstance);
		result.clientInstance = detachedInstance;
		return result;
	}

	private <V extends Entity> V domainImpl(Class<V> clazz,
			long clientInstanceId) {
		long id = clientInstanceId;
		return Domain.find(AlcinaPersistentEntityImpl.getImplementation(clazz),
				id);
	}

	private <V extends Entity> V persistentImpl(EntityManager em, V v) {
		return (V) em.find(v.entityClass(), v.getId());
	}

	public static class BootstrapInstanceCreator
			implements Function<EntityManager, BootstrapCreationResult> {
		@Override
		public BootstrapCreationResult apply(EntityManager em) {
			return AuthenticationPersistence.get()
					.createBootstrapClientInstance(em);
		}
	}

	static class BootstrapCreationResult {
		ClientInstance clientInstance;

		List<Entity> createdDetached = new ArrayList<>();
	}
}
