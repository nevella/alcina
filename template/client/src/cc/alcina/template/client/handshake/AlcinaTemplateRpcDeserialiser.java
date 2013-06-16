package cc.alcina.template.client.handshake;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.persistence.client.RpcDeserialiser;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

@RegistryLocation(registryPoint=RpcDeserialiser.class,implementationType=ImplementationType.SINGLETON)
public class AlcinaTemplateRpcDeserialiser extends RpcDeserialiser {
	@Override
	protected ServiceDefTarget getAsyncService(Class clazz) {
		return (ServiceDefTarget) Registry.impl(AlcinaTemplateRemoteServiceAsync.class);
	}

	@Override
	protected void callServiceInstance(ServiceDefTarget service,
			Class resultClass, AsyncCallback callback) {
		if (resultClass == LoadObjectsHolder.class) {
			((AlcinaTemplateRemoteServiceAsync) service).loadInitial(
					new LoadObjectsRequest(), callback);
			return;
		}
	}
}
