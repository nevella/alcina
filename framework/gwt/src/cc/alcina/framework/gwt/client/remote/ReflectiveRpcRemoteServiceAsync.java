package cc.alcina.framework.gwt.client.remote;

import cc.alcina.framework.common.client.util.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration.EnvironmentRegistration
public interface ReflectiveRpcRemoteServiceAsync {
	void callRpc(String encodedRpcPayload, AsyncCallback<String> callback);
}
