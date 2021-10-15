package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ReflectiveRpcRemoteServiceAsync {
	void callRpc(String encodedRpcPayload, AsyncCallback<String> callback);
}
