package cc.alcina.framework.servlet.module.login;

import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

@RegistryLocation(registryPoint = LoginRequestHandler.class, implementationType = ImplementationType.INSTANCE)
public abstract class LoginRequestHandler<U extends IUser> {
	protected LoginRequest loginRequest;

	protected LoginResponse loginResponse;

	private Authenticator authenticator;

	protected LoginBean loginBean;

	protected LoginModel<U> loginModel;

	public LoginResponse handle(LoginRequest request) {
		this.loginRequest = request;
		loginResponse = new LoginResponse();
		createLoginModel();
		authenticator = createAuthenticator();
		handle0();
		postRequestHandled();
		return loginResponse;
	}

	protected abstract Authenticator createAuthenticator();

	protected void createLoginModel() {
		loginBean = new LoginBean();
		loginBean.setUserName(loginRequest.getUserName());
		loginBean.setPassword(loginRequest.getPassword());
		loginModel = new LoginModel<>();
		loginModel.loginRequest = loginRequest;
		loginModel.loginBean = loginBean;
		loginModel.loginResponse = loginResponse;
	}

	protected void handle0() {
		try {
			if (!validateUserName()) {
				loginResponse.getStates()
						.add(LoginResponseState.Username_not_found);
				return;
			}
			if (loginRequest.getPassword() == null) {
				return;
			}
			if (!validatePassword()) {
				loginResponse.getStates()
						.add(LoginResponseState.Password_incorrect);
				return;
			}
			if (!validateAccount()) {
				loginResponse.getStates()
						.add(LoginResponseState.Account_cannot_login);
				return;
			}
			TwoFactorAuthResult twoFactorAuthResult = validateTwoFactorAuth();
			if (twoFactorAuthResult.requiresTwoFactorAuth) {
				loginResponse.getStates()
						.add(LoginResponseState.Two_factor_code_required);
				loginResponse
						.setTwoFactorAuthQRCode(twoFactorAuthResult.qrCode);
				if (twoFactorAuthResult.requiresTwoFactorQrCode) {
					loginResponse.getStates().add(
							LoginResponseState.Two_factor_qr_code_required);
				}
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

	/*
	 * for instance, remove LoginResponseState.Username_not_found from response
	 * states to meet security requirements
	 */
	protected void postRequestHandled() {
	}

	protected void processLogin() throws Exception {
		authenticator.processValidLogin(
				CommonRemoteServiceServlet.getContextThreadLocalRequest(),
				CommonRemoteServiceServlet.getContextThreadLocalResponse(),
				loginResponse, loginModel.user.getUserName(), true);
	}

	protected boolean validateAccount() throws Exception {
		authenticator.validateAccount(loginModel.loginResponse,
				loginModel.user.getUserName());
		return loginModel.loginResponse.isOk();
	}

	protected boolean validatePassword() {
		return authenticator.validatePassword(loginModel);
	}

	protected TwoFactorAuthResult validateTwoFactorAuth() throws Exception {
		return authenticator.validateTwoFactorAuth(loginModel);
	}

	protected boolean validateUserName() {
		return authenticator.validateUsername(loginModel);
	}

	public static class TwoFactorAuthResult {
		public String qrCode;

		public boolean requiresTwoFactorAuth;

		public boolean requiresTwoFactorQrCode;
	}
}
