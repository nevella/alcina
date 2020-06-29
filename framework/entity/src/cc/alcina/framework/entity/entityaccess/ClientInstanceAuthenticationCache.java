package cc.alcina.framework.entity.entityaccess;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.NullWrappingMap;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = ClientInstanceAuthenticationCache.class, implementationType = ImplementationType.SINGLETON)
public class ClientInstanceAuthenticationCache {
	public static final transient String CONTEXT_IDLE_TIMEOUT_DISABLED = ClientInstanceAuthenticationCache.class
			.getName() + ".CONTEXT_IDLE_TIMEOUT_DISABLED";

	public static ClientInstanceAuthenticationCache get() {
		return Registry.impl(ClientInstanceAuthenticationCache.class);
	}

	HandshakeObjectProvider handshakeObjectProvider;

	private Map<Long, Integer> clientInstanceAuthMap = new NullWrappingMap<Long, Integer>(
			new ConcurrentHashMap());

	private Map<Long, String> clientInstanceUserNameMap = new NullWrappingMap<Long, String>(
			new ConcurrentHashMap());

	private Map<Long, ClientInstance> clientInstanceIdMap = new NullWrappingMap<Long, ClientInstance>(
			new ConcurrentHashMap());

	private Map<Long, String> clientInstanceIidMap = new NullWrappingMap<Long, String>(
			new ConcurrentHashMap());

	private Map<String, String> iidUserNameByKeyMap = new NullWrappingMap<String, String>(
			new ConcurrentHashMap());

	private AccessMap<String> iidLastAccess = new AccessMap<String>(
			(instanceId, time) -> {
				CommonPersistenceBase commonPersistence = handshakeObjectProvider
						.getCommonPersistence();
				if (commonPersistence != null) {
					Iid iid = commonPersistence.getIidByKey(instanceId);
					if (iid == null || iid.getId() == 0) {
						return;
					}
					handshakeObjectProvider.updateIidAccessTime(iid.getId(),
							time);
				}
			});

	private AccessMap<Long> clientInstanceLastAccess = new AccessMap<Long>(
			(clientInstanceId, time) -> handshakeObjectProvider
					.updateClientInstanceAccessTime(clientInstanceId, time));

	public ClientInstance cacheClientInstance(ClientInstance persistent) {
		Class<? extends ClientInstance> clientInstanceImplClass = AlcinaPersistentEntityImpl
				.getImplementation(ClientInstance.class);
		Class<? extends IUser> iUserImplClass = AlcinaPersistentEntityImpl
				.getImplementation(IUser.class);
		ClientInstance forCache = Reflections.classLookup()
				.newInstance(clientInstanceImplClass);
		ResourceUtilities.fieldwiseCopy(persistent, forCache, false, false,
				Collections.singleton("user"));
		forCache.setUser(
				Domain.find(iUserImplClass, persistent.getUser().getId()));
		clientInstanceAuthMap.put(forCache.getId(), forCache.getAuth());
		if (forCache.getUser() != null) {
			clientInstanceUserNameMap.put(forCache.getId(),
					forCache.getUser().getUserName());
		}
		if (forCache.getIid() != null) {
			clientInstanceIidMap.put(forCache.getId(), forCache.getIid());
		}
		clientInstanceLastAccess.setAccessTime(forCache.getId(),
				forCache.getLastAccessed(), false);
		clientInstanceIdMap.put(forCache.getId(), forCache);
		return forCache;
	}

	public void cacheIid(Iid iid, boolean resetAccessTime) {
		if (iid.getInstanceId() == null) {
			return;
		}
		iidUserNameByKeyMap.put(iid.getInstanceId(),
				iid.getRememberMeUser() == null ? null
						: iid.getRememberMeUser().getUserName());
		iidLastAccess.setAccessTime(iid.getInstanceId(), iid.getLastAccessed(),
				resetAccessTime);
	}

	public void cacheUserNameFor(long validatedClientInstanceId,
			String userName) {
		clientInstanceUserNameMap.put(validatedClientInstanceId, userName);
	}

	/**
	 * @return false if expired
	 */
	public boolean checkClientInstanceExpiration(long id) {
		int idleTimeoutSecs = ResourceUtilities.getInteger(
				ClientInstanceAuthenticationCache.class, "idleTimeoutSecs");
		if (idleTimeoutSecs == 0
				|| LooseContext.is(CONTEXT_IDLE_TIMEOUT_DISABLED)) {
			return true;
		}
		String userName = clientInstanceUserNameMap.get(id);
		// only applies to logged in users
		if (userName == null || Objects.equals(userName,
				PermissionsManager.getAnonymousUserName())) {
			return true;
		}
		if (!clientInstanceLastAccess
				.checkAccessTimeAndUpdateIfNotExpired(id)) {
			return false;
		} else {
			String iid = clientInstanceIidMap.get(id);
			if (iid != null) {
				return iidLastAccess.checkAccessTimeAndUpdateIfNotExpired(iid);
			} else {
				return true;
			}
		}
	}

	public boolean containsIIdKey(String iidKey) {
		if (iidKey == null) {
			return false;
		}
		return iidUserNameByKeyMap.containsKey(iidKey);
	}

	public ClientInstance getClientInstance(Long clientInstanceId) {
		return clientInstanceIdMap.get(clientInstanceId);
	}

	public String getIidUserNameByKey(String iid) {
		if (iid == null) {
			return null;
		}
		String userName = iidUserNameByKeyMap.get(iid);
		if (userName == null) {
			return userName;
		}
		if (iidLastAccess.checkAccessTimeAndUpdateIfNotExpired(iid)) {
			return iidUserNameByKeyMap.get(iid);
		} else {
			return null;
		}
	}

	public String getUserNameFor(long validatedClientInstanceId) {
		if (!clientInstanceUserNameMap.containsKey(validatedClientInstanceId)) {
			return null;
		}
		return clientInstanceUserNameMap.get(validatedClientInstanceId);
	}

	public boolean isCached(Long id, Integer auth) {
		return auth != null && id != null
				&& clientInstanceAuthMap.containsKey(id)
				&& auth.intValue() == CommonUtils
						.iv(clientInstanceAuthMap.get(id));
	}

	static class AccessMap<K> {
		private BiConsumer<K, Long> lastAccessPersister;

		Map<K, Long> lastAccessed = Collections
				.synchronizedMap(new LinkedHashMap<>());

		public AccessMap(BiConsumer<K, Long> lastAccessPersister) {
			this.lastAccessPersister = lastAccessPersister;
		}

		public void setAccessTime(K key, Date lastAccessedDate,
				boolean resetAccessTime) {
			if (lastAccessed.containsKey(key) && !resetAccessTime) {
				return;
			}
			long lastAccessedTime = lastAccessedDate == null || resetAccessTime
					? now()
					: lastAccessedDate.getTime();
			lastAccessed.put(key, lastAccessedTime);
			lastAccessPersister.accept(key, lastAccessedTime);
		}

		private long now() {
			return System.currentTimeMillis();
		}

		/**
		 * @return false if expired
		 */
		boolean checkAccessTimeAndUpdateIfNotExpired(K key) {
			long now = System.currentTimeMillis();
			int idleTimeoutSecs = ResourceUtilities.getInteger(
					ClientInstanceAuthenticationCache.class, "idleTimeoutSecs");
			if (idleTimeoutSecs == 0
					|| LooseContext.is(CONTEXT_IDLE_TIMEOUT_DISABLED)) {
			} else {
				Long lastAccess = lastAccessed.get(key);
				if (lastAccess != null && (now - lastAccess > idleTimeoutSecs
						* TimeConstants.ONE_SECOND_MS)) {
					return false;
				}
				if (lastAccess == null || (now - lastAccess)
						* 10 > idleTimeoutSecs * TimeConstants.ONE_SECOND_MS) {
					// greater than 10% delta, persist
					lastAccessPersister.accept(key, now);
				}
				lastAccessed.put(key, now);
			}
			return true;
		}
	}
}
