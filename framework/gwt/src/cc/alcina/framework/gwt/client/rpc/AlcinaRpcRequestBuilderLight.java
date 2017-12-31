package cc.alcina.framework.gwt.client.rpc;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

public abstract class AlcinaRpcRequestBuilderLight extends RpcRequestBuilder {
	public AlcinaRpcRequestBuilderLight() {
	}

	public void addAlcinaHeaders(RequestBuilder rb) {
		rb.setHeader("Cache-Control", "no-cache");
		rb.setHeader(AlcinaRpcRequestBuilder.CLIENT_INSTANCE_ID_KEY,
				getClientInstanceIdString());
		rb.setHeader(AlcinaRpcRequestBuilder.CLIENT_INSTANCE_AUTH_KEY,
				getClientInstanceAuthString());
	}

	@Override
	protected void doFinish(RequestBuilder rb) {
		super.doFinish(rb);
		addAlcinaHeaders(rb);
	}

	protected abstract String getClientInstanceAuthString();

	protected abstract String getClientInstanceIdString();
}
