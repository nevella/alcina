package cc.alcina.framework.servlet.module.login;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = LoginRequestHandler.class, implementationType = ImplementationType.INSTANCE)
public abstract class LoginRequestHandler {
	protected LoginRequest loginRequest;

	protected LoginResponse loginResponse;

	public LoginResponse handle(LoginRequest request) {
		this.loginRequest = request;
		loginResponse = new LoginResponse();
		handle0();
		postRequestHandled();
		return loginResponse;
	}

	/*
	 * for instance, remove LoginResponseState.Username_not_found from response
	 * states to meet security requirements
	 */
	protected abstract void postRequestHandled();

	protected void handle0() {
		try {
			if (!validateUserName()) {
				loginResponse.getStates().add(LoginResponseState.Username_not_found);
				return;
			}
			if (loginRequest.getPassword() == null) {
				return;
			}
			if (!validatePassword()) {
				loginResponse.getStates().add(LoginResponseState.Password_incorrect);
				return;
			}
			if (!validateAccount()) {
				loginResponse.getStates()
						.add(LoginResponseState.Account_cannot_login);
				return;
			}
			processLogin();
			loginResponse.getStates().add(LoginResponseState.Login_complete);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			loginResponse.setErrorMsg(CommonUtils.toSimpleExceptionMessage(e));
			loginResponse.getStates().add(LoginResponseState.Unknown_exception);
		}
	}

	protected abstract void processLogin() throws Exception;

	protected abstract boolean validateUserName();

	protected abstract boolean validatePassword();

	protected abstract boolean validateAccount() throws Exception;
}
