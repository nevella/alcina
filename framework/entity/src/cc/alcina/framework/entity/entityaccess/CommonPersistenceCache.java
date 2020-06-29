package cc.alcina.framework.entity.entityaccess;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

public interface CommonPersistenceCache {
	ClientInstance getClientInstance(Long clientInstanceId);
}
