package cc.alcina.framework.entity.transform.event;

import cc.alcina.framework.servlet.LifecycleService;

public abstract class ExternalTransformPersistenceListener
		extends LifecycleService implements DomainTransformPersistenceListener {
	@Override
	public boolean isAllVmEventsListener() {
		return true;
	}
}
