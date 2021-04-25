package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DevRpcRemoteServiceAsync {
	void devRpc(String encodedRpcPayload, AsyncCallback<String> callback);
}
