package cc.alcina.framework.entity.entityaccess;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;

public interface HandshakeObjectProvider<CI extends ClientInstance, IID extends Iid> {
	public ClientInstance getClientInstance(long clientInstanceId);

	public EntityLocatorMap getLocatorMap(Long clientInstanceId);

	public void setCommonPersistence(CommonPersistenceBase commonPersistence);

	public void updateClientInstanceAccessTime(long clientInstanceId,
			long time);

	CI createClientInstance(String userAgent, String iid, String ipAddress);

	CommonPersistenceBase getCommonPersistence();

	default CI getDetachedClientInstance(Long clientInstanceId) {
		return null;
	}

	void updateIid(String iidKey, String userName, boolean rememberMe);

	void updateIidAccessTime(long iidId, long time);
}
