package cc.alcina.framework.gwt.client.remote;

import cc.alcina.framework.gwt.client.util.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.module.login.LoginRequest;

public interface LoginRemoteServiceAsync {
	void login(LoginRequest request, AsyncCallback<LoginResponse> callback);
}
