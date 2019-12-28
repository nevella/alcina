package cc.alcina.framework.servlet.module.login;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.util.CommonUtils;

@RegistryLocation(registryPoint = LoginRequestHandler.class, implementationType = ImplementationType.INSTANCE)
public abstract class LoginRequestHandler {
	protected LoginRequest request;

	protected LoginResponse response;

	public LoginResponse handle(LoginRequest request) {
		this.request = request;
		response = new LoginResponse();
		handle0(request);
		postRequestHandled();
		return response;
	}

	/*
	 * for instance, remove LoginResponseState.Username_not_found from response
	 * states to meet security requirements
	 */
	protected abstract void postRequestHandled();

	protected void handle0(LoginRequest request) {
		try {
			if (!validateUserName()) {
				response.getStates().add(LoginResponseState.Username_not_found);
				return;
			}
			if (request.getPassword() == null) {
				return;
			}
			if (!validatePassword()) {
				response.getStates().add(LoginResponseState.Password_incorrect);
				return;
			}
			if (!validateAccount()) {
				response.getStates()
						.add(LoginResponseState.Account_cannot_login);
				return;
			}
			response.getStates().add(LoginResponseState.Login_complete);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			response.setErrorMsg(CommonUtils.toSimpleExceptionMessage(e));
			response.getStates().add(LoginResponseState.Unknown_exception);
		}
	}

	protected abstract boolean validateUserName();

	protected abstract boolean validatePassword();

	protected abstract boolean validateAccount() throws Exception;
}
