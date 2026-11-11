package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.util.AsyncCallback;

public interface DomainModelObjectsRegistrar {
	void registerAsync(DomainModelObject domainModelObjects,
			final AsyncCallback<Void> postRegisterCallback);
}
