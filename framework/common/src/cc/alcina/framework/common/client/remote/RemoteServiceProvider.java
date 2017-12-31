package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;

public abstract class RemoteServiceProvider<S> {
	public AlcinaRpcRequestBuilder getRequestBuilder() {
		return new AlcinaRpcRequestBuilder();
	}

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

	protected String adjustEndpoint(String endpoint) {
		if (AlcinaDebugIds.hasFlag(AlcinaDebugIds.DEBUG_SIMULATE_OFFLINE)) {
			endpoint += "-not";
		}
		return endpoint;
	}

	protected abstract S createAndIntialiseEndpoint();
}
