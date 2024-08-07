package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.module.login.LoginRequest;

@Reflected
@Registration.Singleton
public class ReflectiveLoginRemoteServiceAsync
		extends ReflectiveRemoteServiceAsync {
	public static ReflectiveLoginRemoteServiceAsync get() {
		return Registry.impl(ReflectiveLoginRemoteServiceAsync.class);
	}

	public void login(LoginBean request,
			AsyncCallback<LoginResponse> callback) {
		call("login", new Class[] { LoginBean.class }, callback, request);
	}

	public void login(LoginRequest request,
			AsyncCallback<LoginResponse> callback) {
		call("login", new Class[] { LoginRequest.class }, callback, request);
	}
}
