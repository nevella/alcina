package cc.alcina.framework.entity.entityaccess;

import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = ClientInstanceAuthenticationCache.class, implementationType = ImplementationType.SINGLETON)
public class ClientInstanceAuthenticationCache {
	private Map<Long, Integer> clientInstanceAuthMap = new HashMap<Long, Integer>();

	private Map<Long, String> clientInstanceUserNameMap = new HashMap<Long, String>();

	private Map<String, String> iidUserNameByKeyMap = new HashMap<String, String>();

	public synchronized void cacheAuthentication(ClientInstance clientInstance) {
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
		return iidUserNameByKeyMap.get(iid);
	}

	public synchronized void cacheIid(Iid iid) {
		iidUserNameByKeyMap.put(iid.getInstanceId(),
				iid.getRememberMeUser() == null ? null : iid
						.getRememberMeUser().getUserName());
	}

	public boolean containsIIdKey(String iidKey) {
		return iidUserNameByKeyMap.containsKey(iidKey);
	}
}
