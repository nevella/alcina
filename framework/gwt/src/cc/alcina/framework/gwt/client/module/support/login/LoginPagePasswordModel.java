package cc.alcina.framework.gwt.client.module.support.login;

import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.module.login.LoginRequest;
import cc.alcina.framework.gwt.client.gwittir.customiser.PasswordCustomiser;
import cc.alcina.framework.gwt.client.widget.typedbinding.EnumeratedBinding;
import cc.alcina.framework.gwt.client.widget.typedbinding.IntermediateBindable;

@Bean(allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
public class LoginPagePasswordModel extends IntermediateBindable {
	private LoginRequest loginRequest;

	public LoginPagePasswordModel() {
	}

	public LoginPagePasswordModel(LoginRequest loginRequest) {
		super(LoginPagePasswordModelBinding.class);
		this.loginRequest = loginRequest;
	}

	@Display(name = "Password", autocompleteName = "password", focus = true)
	@Custom(customiserClass = PasswordCustomiser.class)
	@Validators(validators = {
			@Validator(validator = NotBlankValidator.class, parameters = {
					@NamedParameter(name = Validator.FEEDBACK_MESSAGE, stringValue = "Password is required") }), })
	public String getPassword() {
		return LoginPagePasswordModelBinding.password.get(this);
	}

	@Override
	public Object provideRelatedObject(Class boundClass) {
		return loginRequest;
	}

	public void setPassword(String password) {
		LoginPagePasswordModelBinding.password.set(this, password);
	}

	public enum LoginPagePasswordModelBinding implements EnumeratedBinding {
		password;
		@Override
		public Class getBoundClass() {
			return LoginRequest.class;
		}
	}
}
