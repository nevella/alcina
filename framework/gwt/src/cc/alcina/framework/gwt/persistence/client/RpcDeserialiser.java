package cc.alcina.framework.gwt.persistence.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;

@Reflected
public abstract class RpcDeserialiser {
	public <T> void deserialize(Class<T> clazz, String payload,
			AsyncCallback<T> callback) {
		ServiceDefTarget service = getAsyncService(clazz);
		AlcinaRpcRequestBuilder rpcRequestBuilder = new AlcinaRpcRequestBuilder();
		rpcRequestBuilder.setResponsePayload(payload);
		service.setRpcRequestBuilder(rpcRequestBuilder);
		callServiceInstance(service, clazz, callback);
	}

	protected abstract void callServiceInstance(ServiceDefTarget service,
			Class resultClass, AsyncCallback callback);

	protected abstract ServiceDefTarget getAsyncService(Class clazz);
}
