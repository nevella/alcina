package cc.alcina.framework.gwt.client.module.support.login;

import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
import cc.alcina.framework.common.client.gwittir.validator.RegexValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.gwt.client.widget.typedbinding.EnumeratedBinding;
import cc.alcina.framework.gwt.client.widget.typedbinding.IntermediateBindable;

@Bean(allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
public class LoginPage2FAModel extends IntermediateBindable {
	private LoginRequest loginRequest;

	public LoginPage2FAModel() {
	}

	public LoginPage2FAModel(LoginRequest loginRequest) {
		super(Login2FAModelBinding.class);
		this.loginRequest = loginRequest;
	}

	@Display(name = "Authentication code", focus = true)
	@Validators(validators = {
			@Validator(validator = NotBlankValidator.class, parameters = {
					@NamedParameter(name = Validator.FEEDBACK_MESSAGE, stringValue = "Two factor code is required") }),
			@Validator(validator = RegexValidator.class, parameters = {
					@NamedParameter(name = Validator.FEEDBACK_MESSAGE, stringValue = "Authenticator code is 6 digits, no spaces"),
					@NamedParameter(name = RegexValidator.PARAM_REGEX, stringValue = "[0-9]{6}") }) })
	public String getTwoFactorAuthenticationCode() {
		return Login2FAModelBinding.twoFactorAuthenticationCode.get(this);
	}

	@Override
	public Object provideRelatedObject(Class boundClass) {
		return loginRequest;
	}

	public void
			setTwoFactorAuthenticationCode(String twoFactorAuthenticationCode) {
		Login2FAModelBinding.twoFactorAuthenticationCode.set(this,
				twoFactorAuthenticationCode);
	}

	public enum Login2FAModelBinding implements EnumeratedBinding {
		twoFactorAuthenticationCode;
		@Override
		public Class getBoundClass() {
			return LoginRequest.class;
		}
	}
}
