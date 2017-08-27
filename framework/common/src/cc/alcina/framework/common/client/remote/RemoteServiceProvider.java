package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;

public abstract class RemoteServiceProvider<S> {
	public S getServiceInstance() {
		S service = createAndIntialiseEndpoint();
		((ServiceDefTarget) service).setRpcRequestBuilder(getRequestBuilder());
		return service;
	}

	public S getServiceInstance(RpcRequestBuilder builder) {
		S service = createAndIntialiseEndpoint();
		((ServiceDefTarget) service).setRpcRequestBuilder(builder);
		return service;
	}

	public AlcinaRpcRequestBuilder getRequestBuilder() {
		return new AlcinaRpcRequestBuilder();
	}

	protected abstract S createAndIntialiseEndpoint();

	protected String adjustEndpoint(String endpoint) {
		if (AlcinaDebugIds.hasFlag(AlcinaDebugIds.DEBUG_SIMULATE_OFFLINE)) {
			endpoint += "-not";
		}
		return endpoint;
	}
}
