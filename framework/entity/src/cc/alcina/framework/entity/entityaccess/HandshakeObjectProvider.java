package cc.alcina.framework.entity.entityaccess;

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

public interface HandshakeObjectProvider<CI extends ClientInstance, IID extends Iid> {
	public void updateIid(String iidKey, String userName, boolean rememberMe);

	public void setCommonPersistence(CommonPersistenceBase commonPersistence);

	CI createClientInstance(String userAgent);
}
