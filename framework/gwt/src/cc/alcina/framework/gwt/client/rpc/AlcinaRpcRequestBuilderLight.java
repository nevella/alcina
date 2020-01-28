package cc.alcina.framework.gwt.client.rpc;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@RegistryLocation(registryPoint = AlcinaRpcRequestBuilderLight.class)
@ClientInstantiable
public abstract class AlcinaRpcRequestBuilderLight extends RpcRequestBuilder {
	public AlcinaRpcRequestBuilderLight() {
	}

	public void addAlcinaHeaders(RequestBuilder rb) {
		rb.setHeader("Cache-Control", "no-cache");
		if (getClientInstanceIdString() != null) {
			rb.setHeader(AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY,
					getClientInstanceIdString());
			rb.setHeader(AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY,
					getClientInstanceAuthString());
		}
	}

	public abstract void adjustEndpoint(ServiceDefTarget serviceDefTarget);

	@Override
	protected void doFinish(RequestBuilder rb) {
		super.doFinish(rb);
		addAlcinaHeaders(rb);
	}

	protected abstract String getClientInstanceAuthString();

	protected abstract String getClientInstanceIdString();
}
