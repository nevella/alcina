package cc.alcina.framework.entity.entityaccess;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.NullWrappingMap;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = ClientInstanceAuthenticationCache.class, implementationType = ImplementationType.SINGLETON)
public class ClientInstanceAuthenticationCache {
	public static final transient String CONTEXT_IDLE_TIMEOUT_DISABLED = ClientInstanceAuthenticationCache.class
			.getName() + ".CONTEXT_IDLE_TIMEOUT_DISABLED";

	private Map<Long, Integer> clientInstanceAuthMap = new NullWrappingMap<Long, Integer>(
			new ConcurrentHashMap());

	private Map<Long, String> clientInstanceUserNameMap = new NullWrappingMap<Long, String>(
			new ConcurrentHashMap());

	private Map<Long, Long> clientInstanceLastAccessedMap = new NullWrappingMap<Long, Long>(
			new ConcurrentHashMap());

	private Map<String, String> iidUserNameByKeyMap = new NullWrappingMap<String, String>(
			new ConcurrentHashMap());

	public void cacheAuthentication(ClientInstance clientInstance) {
		clientInstanceAuthMap.put(clientInstance.getId(),
				clientInstance.getAuth());
		ensureExpirationValues(clientInstance.getId());
		if (clientInstance.getUser() != null) {
			clientInstanceUserNameMap.put(clientInstance.getId(),
					clientInstance.getUser().getUserName());
		}
	}

	public void cacheIid(Iid iid) {
		if (iid.getInstanceId() == null) {
			return;
		}
		iidUserNameByKeyMap.put(iid.getInstanceId(),
				iid.getRememberMeUser() == null ? null
						: iid.getRememberMeUser().getUserName());
	}

	public void cacheUserNameFor(long validatedClientInstanceId,
			String userName) {
		clientInstanceUserNameMap.put(validatedClientInstanceId, userName);
	}

	public boolean checkExpired(long id) {
		int idleTimeoutSecs = ResourceUtilities.getInteger(
				ClientInstanceAuthenticationCache.class, "idleTimeoutSecs");
		if (idleTimeoutSecs == 0
				|| LooseContext.is(CONTEXT_IDLE_TIMEOUT_DISABLED)) {
			return false;
		}
		String userName = clientInstanceUserNameMap.get(id);
		// only applies to logged in users
		if (userName == null || Objects.equals(userName,
				PermissionsManager.getAnonymousUserName())) {
			return false;
		}
		ensureExpirationValues(id);
		long now = System.currentTimeMillis();
		if (now - clientInstanceLastAccessedMap.get(id) > idleTimeoutSecs
				* TimeConstants.ONE_SECOND_MS) {
			return true;
		} else {
			clientInstanceLastAccessedMap.put(id, now);
			return false;
		}
	}

	public boolean containsIIdKey(String iidKey) {
		if (iidKey == null) {
			return false;
		}
		return iidUserNameByKeyMap.containsKey(iidKey);
	}

	public String getUserNameFor(long validatedClientInstanceId) {
		if (!clientInstanceUserNameMap.containsKey(validatedClientInstanceId)) {
			return null;
		}
		return clientInstanceUserNameMap.get(validatedClientInstanceId);
	}

	public String iidUserNameByKey(String iid) {
		if (iid == null || !iidUserNameByKeyMap.containsKey(iid)) {
			return null;
		}
		return iidUserNameByKeyMap.get(iid);
	}

	public boolean isCached(Long id, Integer auth) {
		return auth != null && id != null
				&& clientInstanceAuthMap.containsKey(id)
				&& auth.intValue() == CommonUtils
						.iv(clientInstanceAuthMap.get(id));
	}

	private void ensureExpirationValues(long id) {
		clientInstanceLastAccessedMap.putIfAbsent(id,
				System.currentTimeMillis());
	}
}
