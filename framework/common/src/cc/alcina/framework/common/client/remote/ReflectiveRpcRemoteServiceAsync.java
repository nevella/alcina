package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration.EnvironmentRegistration
public interface ReflectiveRpcRemoteServiceAsync {
	void callRpc(String encodedRpcPayload, AsyncCallback<String> callback);
}
