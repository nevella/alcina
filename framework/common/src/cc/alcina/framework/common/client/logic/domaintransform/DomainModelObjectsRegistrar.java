package cc.alcina.framework.common.client.logic.domaintransform;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DomainModelObjectsRegistrar {

	void registerAsync(DomainModelObjects domainModelObjects,final AsyncCallback<Void> postRegisterCallback);
}
