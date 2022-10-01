package cc.alcina.framework.gwt.client.module.support.login;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.common.client.gwittir.validator.EmailAddressValidator;
import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.gwt.client.dirndl.model.Editable;

public class LoginPageUsername extends LoginPage {
	protected Editable.StringInput input;

	public LoginPageUsername(LoginConsort loginConsort) {
		super(loginConsort);
		input = new Editable.StringInput();
		input.setFocusOnAttach(true);
		input.setPlaceholder("ABA email address");
		setContents(input);
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
	protected String getEnteredText() {
		input.sync();
		return input.getValue();
	}

	@Override
	protected Validator getValidator() {
		CompositeValidator validator = new CompositeValidator()
				.add(new NotNullValidator()).add(new EmailAddressValidator());
		return validator;
	}

	@Override
	protected void onForwardValidated() {
		loginConsort.request.setUserName(getEnteredText());
		super.onForwardValidated();
	}
}
