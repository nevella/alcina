package cc.alcina.framework.entity.transform.event;

import cc.alcina.framework.common.client.logic.reflection.registry.LifecycleService;

public interface ExternalTransformPersistenceListener
		extends DomainTransformPersistenceListener, LifecycleService {
	@Override
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent evt);

	@Override
	public void startService();

	@Override
	public void stopService();
}
