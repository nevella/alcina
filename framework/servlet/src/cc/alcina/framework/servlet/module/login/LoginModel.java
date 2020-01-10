package cc.alcina.framework.servlet.module.login;

import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.module.login.LoginRequest;

public class LoginModel<U extends IUser> {
	public LoginBean loginBean;

	public U user;

	public LoginResponse loginResponse;

	public LoginRequest loginRequest;
}