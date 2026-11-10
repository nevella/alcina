package cc.alcina.framework.common.client.remote;

import com.google.gwt.user.client.rpc.RemoteService;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.module.login.LoginRequest;

public interface LoginRemoteService extends RemoteService {
	@WebMethod
	public LoginResponse login(LoginRequest request);
}
