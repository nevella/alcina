package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.module.login.LoginRequest;

public interface LoginRemoteServiceAsync {
	void login(LoginRequest request, AsyncCallback<LoginResponse> callback);
}
