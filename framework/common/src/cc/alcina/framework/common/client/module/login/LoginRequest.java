package cc.alcina.framework.common.client.module.login;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Display;

public class LoginRequest extends Bindable {
	private String userName;

	private String password;

	private boolean rememberMe;

	private boolean recoverPassword;

	private String twoFactorAuthenticationCode;

	private Map<String, String> properties = new LinkedHashMap<>();

	public LoginRequest() {
	}

	@Display(autocompleteName = "password")
	public String getPassword() {
		return this.password;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public String getTwoFactorAuthenticationCode() {
		return this.twoFactorAuthenticationCode;
	}

	@Display(autocompleteName = "email")
	public String getUserName() {
		return this.userName;
	}

	public boolean isRecoverPassword() {
		return this.recoverPassword;
	}

	@Display
	public boolean isRememberMe() {
		return this.rememberMe;
	}

	public void setPassword(String password) {
		String old_password = this.password;
		this.password = password;
		propertyChangeSupport().firePropertyChange("password", old_password,
				password);
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public void setRecoverPassword(boolean recoverPassword) {
		boolean old_recoverPassword = this.recoverPassword;
		this.recoverPassword = recoverPassword;
		propertyChangeSupport().firePropertyChange("recoverPassword",
				old_recoverPassword, recoverPassword);
	}

	public void setRememberMe(boolean rememberMe) {
		boolean old_rememberMe = this.rememberMe;
		this.rememberMe = rememberMe;
		propertyChangeSupport().firePropertyChange("rememberMe", old_rememberMe,
				rememberMe);
	}

	public void
			setTwoFactorAuthenticationCode(String twoFactorAuthenticationCode) {
		String old_twoFactorAuthenticationCode = this.twoFactorAuthenticationCode;
		this.twoFactorAuthenticationCode = twoFactorAuthenticationCode;
		propertyChangeSupport().firePropertyChange(
				"twoFactorAuthenticationCode", old_twoFactorAuthenticationCode,
				twoFactorAuthenticationCode);
	}

	public void setUserName(String userName) {
		String old_userName = this.userName;
		this.userName = userName;
		propertyChangeSupport().firePropertyChange("userName", old_userName,
				userName);
	}
}
