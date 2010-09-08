package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

public class AlcinaRpcRequestBuilder extends RpcRequestBuilder {
	public static final String CLIENT_INSTANCE_ID_KEY = "X-ALCINA-CLIENT-INSTANCE-ID";

	public static final String CLIENT_INSTANCE_AUTH_KEY = "X-ALCINA-CLIENT-INSTANCE-AUTH";

	@Override
	protected RequestBuilder doCreate(String serviceEntryPoint) {
		// TODO Auto-generated method stub
		return super.doCreate(serviceEntryPoint);
	}

	@Override
	protected void doFinish(RequestBuilder rb) {
		super.doFinish(rb);
		if (ClientLayerLocator.get().getClientInstance() != null) {
			rb.setHeader(CLIENT_INSTANCE_ID_KEY, String
					.valueOf(ClientLayerLocator.get().getClientInstance()
							.getId()));
			rb.setHeader(CLIENT_INSTANCE_AUTH_KEY, ClientLayerLocator.get()
					.getClientInstance().getAuth().toString());
		}
	}
}
