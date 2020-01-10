package cc.alcina.framework.servlet.module.login;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lambdaworks.crypto.SCrypt;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.UserWith2FA;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.CookieHelper;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.authentication.AuthenticationException;
import cc.alcina.framework.servlet.module.login.LoginRequestHandler.TwoFactorAuthResult;

public abstract class Authenticator<U extends IUser> {
	public static final String CONTEXT_BYPASS_PASSWORD_CHECK = Authenticator.class
			.getName() + ".CONTEXT_BYPASS_PASSWORD_CHECK";

	public Authenticator() {
		super();
	}

	public LoginResponse authenticate(LoginBean loginBean)
			throws AuthenticationException {
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setOk(false);
		LoginModel loginModel = new LoginModel();
		loginModel.loginBean = loginBean;
		loginModel.loginResponse = loginResponse;
		authenticate(loginBean, loginModel);
		return loginResponse;
	}

	public U createUser(String userName, String password) {
		U user = Domain.create(getUserClass());
		user.setUserName(userName);
		setPassword(user, password);
		return user;
	}

	public void processValidLogin(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			LoginResponse loginResponse, String userName, boolean rememberMe)
			throws AuthenticationException {
		U user = validateAccount(loginResponse, userName);
		if (loginResponse.isOk()) {
			Registry.impl(SessionHelper.class).setupSessionForUser(
					httpServletRequest, httpServletResponse, user);
			if (user instanceof UserWith2FA) {
				((UserWith2FA) user).setHasSuccessfullyLoggedIn(true);
				Sx.commit();
			}
		}
		createClientInstance(httpServletRequest, httpServletResponse,
				loginResponse);
		if (rememberMe) {
			new CookieHelper().setRememberMeCookie(httpServletRequest,
					httpServletResponse, rememberMe);
		}
	}

	public void setPassword(U user, String password) {
		if (user.getSalt() == null) {
			user.setSalt(user.getUserName());
		}
		user.setPassword(
				ScryptSupport.encryptPassword(user.getSalt(), password));
	}

	public abstract U validateAccount(LoginResponse loginResponse,
			String userName) throws AuthenticationException;

	public TwoFactorAuthResult validateTwoFactorAuth(LoginModel<U> loginModel)
			throws Exception {
		TwoFactorAuthResult result = new TwoFactorAuthResult();
		result.requiresTwoFactorAuth = false;
		if (appUsesTwoFactorAuthentication()) {
			result.requiresTwoFactorAuth = true;
			UserWith2FA user = (UserWith2FA) loginModel.user;
			if (Ax.notBlank(
					loginModel.loginRequest.getTwoFactorAuthenticationCode())) {
				long t = new Date().getTime() / TimeUnit.SECONDS.toMillis(30);
				if (new TwoFactorAuthentication()
						.checkCode(user.getAuthenticationSecret(),
								Long.parseLong(loginModel.loginRequest
										.getTwoFactorAuthenticationCode()),
								t)) {
					result.requiresTwoFactorAuth = false;
				} else {
					loginModel.loginResponse
							.setErrorMsg("Invalid authentication code");
				}
			}
			if (result.requiresTwoFactorAuth) {
				if (!((UserWith2FA) loginModel.user)
						.isHasSuccessfullyLoggedIn()) {
					result.requiresTwoFactorQrCode = true;
				}
				result.qrCode = new TwoFactorAuthentication().getQRBarcodeURL(
						loginModel.user.getUserName(),
						EntityLayerUtils.getApplicationHostName(),
						user.getAuthenticationSecret());
			}
		}
		return result;
	}

	protected boolean appUsesTwoFactorAuthentication() {
		return false;
	}

	protected abstract void createClientInstance(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			LoginResponse loginResponse);

	protected abstract U getUser(String userName);

	protected abstract Class<U> getUserClass();

	protected boolean validatePassword(LoginModel<U> loginModel) {
		U user = loginModel.user;
		if (user.getSalt() == null) {
			user.setSalt(user.getUserName());
		}
		if (user instanceof UserWith2FA
				&& ((UserWith2FA) user).getAuthenticationSecret() == null) {
			((UserWith2FA) user).setAuthenticationSecret(
					new TwoFactorAuthentication().generateSecret());
		}
		Sx.commit();
		if (!LooseContext.is(CONTEXT_BYPASS_PASSWORD_CHECK) && !ScryptSupport
				.encryptPassword(user.getSalt(),
						loginModel.loginBean.getPassword())
				.equals(user.getPasswordHash())) {
			loginModel.loginResponse.setErrorMsg("Password incorrect");
			return false;
		} else {
			return true;
		}
	}

	protected boolean validateUsername(LoginModel<U> loginModel) {
		String userName = loginModel.loginBean.getUserName();
		loginModel.user = getUser(userName);
		if (loginModel.user == null) {
			loginModel.loginResponse
					.setErrorMsg("Email address not registered");
		}
		return loginModel.user != null;
	}

	void authenticate(LoginBean loginBean, LoginModel loginModel)
			throws AuthenticationException {
		if (!validateUsername(loginModel)) {
			return;
		}
		if (!validatePassword(loginModel)) {
			return;
		}
		validateAccount(loginModel.loginResponse, loginBean.getUserName());
	}

	protected static class ScryptSupport {
		private static final int N = 16384;

		private static final int r = 8;

		private static final int p = 1;

		private static final int dkLen = 64;

		static String encryptPassword(String salt, String password) {
			try {
				byte[] scrypt = SCrypt.scrypt(password.getBytes("UTF-8"),
						salt.getBytes("UTF-8"), N, r, p, dkLen);
				return Base64Utils.toBase64(scrypt);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}