package cc.alcina.template.client;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsyncProvider;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteService;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

@RegistryLocations({
		@RegistryLocation(registryPoint = CommonRemoteServiceAsyncProvider.class, priority = RegistryLocation.MANUAL_PRIORITY, implementationType = ImplementationType.SINGLETON),
		@RegistryLocation(registryPoint = AlcinaTemplateRemoteServiceAsync.class, priority = RegistryLocation.MANUAL_PRIORITY, implementationType = ImplementationType.FACTORY),
		@RegistryLocation(registryPoint = CommonRemoteServiceAsync.class, priority = RegistryLocation.MANUAL_PRIORITY, implementationType = ImplementationType.FACTORY) })
@ClientInstantiable
public class AlcinaTemplateRemoteServiceAsyncProvider extends
		CommonRemoteServiceAsyncProvider<AlcinaTemplateRemoteServiceAsync>
		implements RegistryFactory<AlcinaTemplateRemoteServiceAsync> {
	public AlcinaRpcRequestBuilder getRequestBuilder() {
		return new AlcinaRpcRequestBuilder();
	}

	@Override
	protected AlcinaTemplateRemoteServiceAsync createAndIntialiseEndpoint() {
		AlcinaTemplateRemoteServiceAsync service = (AlcinaTemplateRemoteServiceAsync) GWT
				.create(AlcinaTemplateRemoteService.class);
		((ServiceDefTarget) service)
				.setServiceEntryPoint(adjustEndpoint("/alcinaTemplateService.do"));
		return service;
	}

	@Override
	public AlcinaTemplateRemoteServiceAsync create(
			Class<? extends AlcinaTemplateRemoteServiceAsync> registryPoint,
			Class targetObjectClass) {
		return getServiceInstance();
	}
}