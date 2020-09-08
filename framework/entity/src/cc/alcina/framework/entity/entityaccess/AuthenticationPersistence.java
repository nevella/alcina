package cc.alcina.framework.entity.entityaccess;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.persistence.EntityManager;

import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.Iid;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit;
import cc.alcina.framework.entity.logic.EntityLayerUtils;

@RegistryLocation(registryPoint = AuthenticationPersistence.class, implementationType = ImplementationType.SINGLETON)
public class AuthenticationPersistence {
	public static final transient String CONTEXT_IDLE_TIMEOUT_DISABLED = AuthenticationPersistence.class
			.getName() + ".CONTEXT_IDLE_TIMEOUT_DISABLED";

	public static AuthenticationPersistence get() {
		return Registry.impl(AuthenticationPersistence.class);
	}

	public ClientInstance getClientInstance(long clientInstanceId) {
		return domainImpl(ClientInstance.class, clientInstanceId);
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

	public ClientInstance createClientInstance(AuthenticationSession session,
			String userAgent, String remoteAddress, String referrer,
			String url) {
		Class<? extends ClientInstance> clazz = AlcinaPersistentEntityImpl
				.getImplementation(ClientInstance.class);
		ClientInstance clientInstance = Domain.create(clazz);
		clientInstance.setAuthenticationSession(session);
		clientInstance.setHelloDate(new Date());
		clientInstance.setUserAgent(userAgent);
		clientInstance.setAuth(Math.abs(new Random().nextInt()));
		clientInstance.setIpAddress(remoteAddress);
		clientInstance.setReferrer(referrer);
		clientInstance.setUrl(url);
		clientInstance
				.setBotUserAgent(EntityLayerUtils.isBotUserAgent(userAgent));
		return clientInstance;
	}

	@SuppressWarnings("unused")
	private Map<String, String> lookupQueries = new ConcurrentHashMap<>();

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
//		if (lookupQueries.put(query, query) != null) {
//			return null;
//		}
		Long id = Ax.first((List<Long>) callWithEntityManager(
				em -> em.createQuery(query).getResultList()));
		return id == null ? null : Domain.find(clazz, id);
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
//		if (lookupQueries.put(query, query) != null) {
//			return null;
//		}
		Long id = Ax.first((List<Long>) callWithEntityManager(
				em -> em.createQuery(query).getResultList()));
		if (id == null) {
			return null;
		}
		AuthenticationSession session = Domain.find(clazz, id);
		return session;
	}

	public AuthenticationSession createAuthenticationSession(Iid iid,
			Date startDate, String sessionId, IUser user,
			String authenticationType) {
		Class<? extends AuthenticationSession> clazz = AlcinaPersistentEntityImpl
				.getImplementation(AuthenticationSession.class);
		AuthenticationSession session = Domain.create(clazz);
		if(iid.getVersionNumber()==-1){
			iid.setVersionNumber(0);
			String eql = Ax.format("update iid set optlock=0 where id=%s",iid.getId());
			callWithEntityManager(em->{
				em.createNativeQuery(eql).executeUpdate();
				//
				return  null;
			});
		}
		session.setIid(iid);
		session.setSessionId(sessionId);
		session.setUser(user);
		session.setAuthenticationType(authenticationType);
		session.setStartTime(startDate);
		return session;
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

	public static class BootstrapInstanceCreator implements Function<EntityManager, ClientInstance>{

		@Override
		public ClientInstance apply(EntityManager em) {
			return AuthenticationPersistence
					.get().createBootstrapClientInstance(em);
		}
		
	}
	public ClientInstance createBootstrapClientInstance() {
		ClientInstance persistent = callWithEntityManager(
				new BootstrapInstanceCreator());
		Domain.find(persistent.getAuthenticationSession().getIid());
		Domain.find(persistent.getAuthenticationSession());
		return Domain.find(persistent);
	}

	private ClientInstance createBootstrapClientInstance(EntityManager em) {
		String hostName = EntityLayerUtils.getLocalHostName();
		String authenticationSessionUid = Ax.format("servlet:%s", hostName);
		String iidUid = authenticationSessionUid;
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
		AuthenticationSession detachedSession = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(AuthenticationSession.class);
		detachedSession.setId(authenticationSession.getId());
		detachedSession.setIid(detachedIid);
		ClientInstance detachedInstance = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(ClientInstance.class);
		detachedInstance.setId(clientInstance.getId());
		detachedInstance.setAuthenticationSession(detachedSession);
		return detachedInstance;
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

	private Logger logger = LoggerFactory.getLogger(getClass());

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
}
