package cc.alcina.framework.servlet.cluster.transform;

import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

public interface ExternalTransformPersistenceListener
		extends DomainTransformPersistenceListener {
	@Override
	default boolean isAllVmEventsListener() {
		return true;
	}
}
