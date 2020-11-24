package cc.alcina.framework.gwt.client.module.support.login;

import cc.alcina.framework.common.client.gwittir.validator.EmailAddressValidator;
import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
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
public class LoginPageUsernameModel extends IntermediateBindable {
	private LoginRequest loginRequest;

	public LoginPageUsernameModel() {
	}

	public LoginPageUsernameModel(LoginRequest loginRequest) {
		super(LoginPageUsernameModelBinding.class);
		this.loginRequest = loginRequest;
	}

	@Display(name = "Email", autocompleteName = "email", focus = true)
	@Validators(validators = {
			@Validator(validator = NotBlankValidator.class, parameters = {
					@NamedParameter(name = Validator.FEEDBACK_MESSAGE, stringValue = "An email is required") }),
			@Validator(validator = EmailAddressValidator.class, parameters = {
					@NamedParameter(name = EmailAddressValidator.PARAM_IGNORE_EMPTIES, booleanValue = true) }) })
	public String getUserName() {
		return LoginPageUsernameModelBinding.userName.get(this);
	}

	@Override
	public Object provideRelatedObject(Class boundClass) {
		return loginRequest;
	}

	public void setUserName(String userName) {
		LoginPageUsernameModelBinding.userName.set(this, userName);
	}

	public enum LoginPageUsernameModelBinding implements EnumeratedBinding {
		userName;
		@Override
		public Class getBoundClass() {
			return LoginRequest.class;
		}
	}
}
