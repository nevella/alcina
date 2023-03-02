package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.user.client.History;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.gwittir.validator.EmailAddressValidator;
import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Cancel;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;

@Directed(receives = ModelEvents.Cancel.class)
public class LoginPageUsername extends LoginPage
		implements ModelEvents.Cancel.Handler {
	protected StringInput input;

	public LoginPageUsername(LoginConsort loginConsort) {
		super(loginConsort);
		input = new StringInput();
		input.setFocusOnBind(true);
		input.setAutocomplete("username");
		input.setPlaceholder(getEmailAddress());
		setContents(input);
	}

	@Override
	public void onCancel(Cancel event) {
		History.back();
	}

	protected String getEmailAddress() {
		return "Email address";
	}

	@Override
	protected String getEnteredText() {
		return input.getValue();
	}

	@Override
	protected String getMessage(ValidationException e) {
		if (e.getValidatorClass() == NotNullValidator.class) {
			return "Email is required";
		}
		return super.getMessage(e);
	}

	@Override
	protected String getSubtitleText() {
		return loginConsort.getUsernamePageSubtitleText();
	}

	@Override
	protected Validator getValidator() {
		Validator validator = loginConsort.isRequiresValidEmail()
				? new CompositeValidator().add(new NotNullValidator())
						.add(new EmailAddressValidator())
				: new NotNullValidator();
		return validator;
	}

	@Override
	protected void onForwardValidated() {
		loginConsort.request.setUserName(getEnteredText());
		super.onForwardValidated();
	}
}
