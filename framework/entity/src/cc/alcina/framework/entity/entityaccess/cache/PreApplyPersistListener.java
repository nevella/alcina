package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;

public interface PreApplyPersistListener {
	public void loadLazyPreApplyPersist(
			DomainTransformPersistenceEvent persistenceEvent) throws Exception ;
}
