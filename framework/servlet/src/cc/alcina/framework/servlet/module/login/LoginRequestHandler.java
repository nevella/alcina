package cc.alcina.framework.servlet.module.login;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.servlet.authentication.Authenticator;

@Registration(LoginRequestHandler.class)
public abstract class LoginRequestHandler<U extends IUser> {
	protected LoginRequest loginRequest;

	protected LoginResponse loginResponse;

	private Authenticator authenticator;

	protected LoginBean loginBean;

	protected LoginModel<U> loginModel;

	Logger logger = LoggerFactory.getLogger(getClass());

	protected abstract Authenticator createAuthenticator();

	protected void createLoginModel() {
		loginBean = new LoginBean();
		loginBean.setUserName(loginRequest.getUserName());
		loginBean.setPassword(loginRequest.getPassword());
		loginBean.setProperties(loginRequest.getProperties());
		loginModel = new LoginModel<>();
		loginModel.loginRequest = loginRequest;
		loginModel.loginBean = loginBean;
		loginModel.loginResponse = loginResponse;
	}

	public LoginResponse handle(LoginRequest request) {
		this.loginRequest = request;
		loginResponse = new LoginResponse();
		createLoginModel();
		authenticator = createAuthenticator();
		handle0();
		postRequestHandled();
		return loginResponse;
	}

	protected void handle0() {
		Set<LoginResponseState> states = loginResponse.getStates();
		try {
			if (!validateLoginAttempt()) {
				states.add(LoginResponseState.Account_locked_out);
				return;
			}
			if (!validateUserName()) {
				states.add(LoginResponseState.Username_not_found);
				return;
			} else {
				states.add(LoginResponseState.Username_found);
			}
			if (loginRequest.getPassword() == null) {
				return;
			}
			if (!validatePassword()) {
				states.add(LoginResponseState.Password_incorrect);
				return;
			}
			if (!validateAccount()) {
				states.add(LoginResponseState.Account_cannot_login);
				return;
			}
			TwoFactorAuthResult twoFactorAuthResult = validateTwoFactorAuth();
			if (twoFactorAuthResult.requiresTwoFactorAuth) {
				states.add(LoginResponseState.Two_factor_code_required);
				loginResponse
						.setTwoFactorAuthQRCode(twoFactorAuthResult.qrCode);
				if (twoFactorAuthResult.requiresTwoFactorQrCode) {
					states.add(LoginResponseState.Two_factor_qr_code_required);
				}
				return;
			}
			processLogin();
			states.add(LoginResponseState.Login_complete);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			loginResponse.setErrorMsg(CommonUtils.toSimpleExceptionMessage(e));
			states.add(LoginResponseState.Unknown_exception);
		}
	}

	/*
	 * for instance, remove LoginResponseState.Username_not_found from response
	 * states to meet security requirements
	 */
	protected void postRequestHandled() {
		if (Configuration.is("recordLoginAttempts")) {
			new LoginAttempts().handleLoginResult(loginModel);
		}
		if (loginResponse.isOk()) {
			logger.info("Logged in: {}", loginRequest.getUserName());
		} else {
			logger.warn("Login failed: {} {}", loginRequest.getUserName(),
					loginResponse.getStates());
		}
	}

	protected void processLogin() throws Exception {
		authenticator.processValidLogin(loginResponse,
				loginModel.user.getUserName(), true);
	}

	protected boolean validateAccount() throws Exception {
		authenticator.validateAccount(loginModel.loginResponse,
				loginModel.user.getUserName());
		return loginModel.loginResponse.isOk();
	}

	protected boolean validateLoginAttempt() {
		return authenticator.validateLoginAttempt(loginModel);
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
