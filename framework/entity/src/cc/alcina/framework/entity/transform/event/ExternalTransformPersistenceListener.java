package cc.alcina.framework.entity.transform.event;

import cc.alcina.framework.common.client.logic.reflection.registry.LifecycleService;

public interface ExternalTransformPersistenceListener
		extends DomainTransformPersistenceListener, LifecycleService {
	@Override
	default boolean isAllVmEventsListener() {
		return true;
	}
}
