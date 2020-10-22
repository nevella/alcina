package cc.alcina.framework.servlet.authentication;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.crypto.SCrypt;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.UserWith2FA;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.module.login.LoginAttempts;
import cc.alcina.framework.servlet.module.login.LoginModel;
import cc.alcina.framework.servlet.module.login.LoginRequestHandler.TwoFactorAuthResult;
import cc.alcina.framework.servlet.module.login.TwoFactorAuthentication;

@RegistryLocation(registryPoint = Authenticator.class, implementationType = ImplementationType.INSTANCE)
public abstract class Authenticator<U extends Entity & IUser> {
	public static final String CONTEXT_BYPASS_PASSWORD_CHECK = Authenticator.class
			.getName() + ".CONTEXT_BYPASS_PASSWORD_CHECK";

	public static Authenticator get() {
		return Registry.impl(Authenticator.class);
	}

	protected LoginModel loginModel;

	public Authenticator() {
		super();
	}

	public LoginResponse authenticate(LoginBean loginBean)
			throws AuthenticationException {
		LoginResponse loginResponse = new LoginResponse();
		loginResponse.setOk(false);
		loginModel = new LoginModel();
		loginModel.loginBean = loginBean;
		loginModel.loginResponse = loginResponse;
		authenticate(loginBean, loginModel);
		return loginResponse;
	}

	public void authenticate(LoginBean loginBean, LoginModel loginModel)
			throws AuthenticationException {
		if (!validateUsername(loginModel)) {
			return;
		}
		if (!validatePassword(loginModel)) {
			return;
		}
		validateAccount(loginModel.loginResponse, loginBean.getUserName());
	}

	public U createUser(String userName, String password) {
		U user = (U) Domain.create((Class) AlcinaPersistentEntityImpl
				.getImplementation(IUser.class));
		user.setUserName(userName);
		setPassword(user, password);
		return user;
	}

	public void logOut() {
		AuthenticationManager.get().createAuthenticationSession(new Date(),
				UserlandProvider.get().getAnonymousUser(), "logout", false);
		Transaction.commit();
	}

	public void processValidLogin(LoginResponse loginResponse, String userName,
			boolean rememberMe) throws AuthenticationException {
		if (loginModel == null) {
			LoginBean loginBean = new LoginBean();
			loginBean.setUserName(userName);
			loginModel = new LoginModel();
			loginModel.loginBean = loginBean;
			loginModel.loginResponse = loginResponse;
		}
		U user = validateAccount(loginResponse, userName);
		if (loginResponse.isOk()) {
			AuthenticationSession authenticationSession = AuthenticationManager
					.get().createAuthenticationSession(new Date(), user,
							"password", true);
			if (!rememberMe) {
				authenticationSession.setMaxInstances(1);
			}
			if (user instanceof UserWith2FA) {
				((UserWith2FA) user).setHasSuccessfullyLoggedIn(true);
			}
			Transaction.commit();
		}
	}

	public void setPassword(U user, String password) {
		if (Ax.isBlank(user.getSalt())) {
			user.setSalt(user.getUserName());
		}
		user.setPassword(PasswordEncryptionSupport.get()
				.encryptPassword(password, user.getSalt()));
	}

	public abstract U validateAccount(LoginResponse loginResponse,
			String userName) throws AuthenticationException;

	public boolean validateLoginAttempt(LoginModel<U> loginModel) {
		if (ResourceUtilities.is(Authenticator.class,
				"validateLoginAttempts")) {
			return new LoginAttempts().checkLockedOut(loginModel);
		} else {
			return true;
		}
	}

	public boolean validatePassword(LoginModel<U> loginModel) {
		U user = loginModel.user;
		if (user.getSalt() == null) {
			user.setSalt(user.getUserName());
		}
		if (user instanceof UserWith2FA
				&& ((UserWith2FA) user).getAuthenticationSecret() == null) {
			((UserWith2FA) user).setAuthenticationSecret(
					new TwoFactorAuthentication().generateSecret());
		}
		Transaction.commit();
		if (!LooseContext.is(CONTEXT_BYPASS_PASSWORD_CHECK)
				&& !PasswordEncryptionSupport.get().check(
						loginModel.loginBean.getPassword(), user.getSalt(),
						user.getPasswordHash())) {
			loginModel.loginResponse.setErrorMsg("Password incorrect");
			return false;
		} else {
			return true;
		}
	}

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

	public boolean validateUsername(LoginModel<U> loginModel) {
		String userName = loginModel.loginBean.getUserName();
		loginModel.user = UserlandProvider.get().getUserByName(userName);
		if (loginModel.user == null) {
			loginModel.loginResponse
					.setErrorMsg("Email address not registered");
		}
		return loginModel.user != null;
	}

	protected boolean appUsesTwoFactorAuthentication() {
		return false;
	}

	protected boolean
			validateLoginAttemptFromHistory(LoginModel<U> loginModel) {
		return true;
	}

	public interface PasswordEncryptionSupport {
		public static PasswordEncryptionSupport get() {
			return Registry.impl(PasswordEncryptionSupport.class);
		}

		default boolean check(String password, String salt,
				String hashedPassword) {
			return encryptPassword(password, salt).equals(hashedPassword);
		}

		String encryptPassword(String password, String salt);

		default String maybeReencrypt(String salt, String hashedPassword)
				throws Exception {
			return hashedPassword;
		}
	}

	@RegistryLocation(registryPoint = PasswordEncryptionSupport.class, implementationType = ImplementationType.INSTANCE)
	public static class ScryptSupport implements PasswordEncryptionSupport {
		private static final int N = 16384;

		private static final int r = 8;

		private static final int p = 1;

		private static final int dkLen = 64;

		@Override
		public String encryptPassword(String password, String salt) {
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