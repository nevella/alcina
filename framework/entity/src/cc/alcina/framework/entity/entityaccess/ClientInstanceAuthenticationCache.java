package cc.alcina.framework.entity.entityaccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NullWrappingMap;

@RegistryLocation(registryPoint = ClientInstanceAuthenticationCache.class, implementationType = ImplementationType.SINGLETON)
public class ClientInstanceAuthenticationCache {
	private Map<Long, Integer> clientInstanceAuthMap = new NullWrappingMap<Long, Integer>(
			new ConcurrentHashMap());

	private Map<Long, String> clientInstanceUserNameMap = new NullWrappingMap<Long, String>(
			new ConcurrentHashMap());

	private Map<String, String> iidUserNameByKeyMap = new NullWrappingMap<String, String>(
			new ConcurrentHashMap());

	public void cacheAuthentication(ClientInstance clientInstance) {
		clientInstanceAuthMap.put(clientInstance.getId(),
				clientInstance.getAuth());
		if (clientInstance.getUser() != null) {
			clientInstanceUserNameMap.put(clientInstance.getId(),
					clientInstance.getUser().getUserName());
		}
	}

	public boolean isCached(Long id, Integer auth) {
		return auth != null
				&& auth.intValue() == CommonUtils.iv(clientInstanceAuthMap
						.get(id));
	}

	public String getUserNameFor(long validatedClientInstanceId) {
		return clientInstanceUserNameMap.get(validatedClientInstanceId);
	}

	public String iidUserNameByKey(String iid) {
		if (iid == null) {
			return null;
		}
		return iidUserNameByKeyMap.get(iid);
	}

	public void cacheIid(Iid iid) {
		if (iid.getInstanceId() == null) {
			return;
		}
		iidUserNameByKeyMap.put(iid.getInstanceId(),
				iid.getRememberMeUser() == null ? null : iid
						.getRememberMeUser().getUserName());
	}

	public boolean containsIIdKey(String iidKey) {
		if (iidKey == null) {
			return false;
		}
		return iidUserNameByKeyMap.containsKey(iidKey);
	}
}
