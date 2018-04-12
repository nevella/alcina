package cc.alcina.framework.entity.entityaccess;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;

public interface HandshakeObjectProvider<CI extends ClientInstance, IID extends Iid> {
	public ClientInstance getClientInstance(long clientInstanceId);

	public void setCommonPersistence(CommonPersistenceBase commonPersistence);

	public void updateIid(String iidKey, String userName, boolean rememberMe);

	CI createClientInstance(String userAgent, String iid, String ipAddress);

	public HiliLocatorMap getLocatorMap(Long clientInstanceId);
}
