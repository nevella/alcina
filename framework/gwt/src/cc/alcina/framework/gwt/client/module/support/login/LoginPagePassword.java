package cc.alcina.framework.gwt.client.module.support.login;

import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;

public class LoginPagePassword extends LoginPage {
	private StringInput input;

	public LoginPagePassword(LoginConsort loginConsort) {
		super(loginConsort);
		input = new StringInput();
		input.setFocusOnBind(true);
		input.setType("password");
		input.setPlaceholder("Password");
		setContents(input);
		// TODO
		// loginConsort.topicCallingRemote.add(calling -> {
		// if (!calling && isAttached()) {
		// buttonsPanel.removeOptionalButtons();
		// if (!loginConsort.lastResponse.isOk()) {
		// buttonsPanel.addOptionalButton(
		// new LuxButton().withText("Back").withHandler(e -> {
		// loginConsort.request.setPassword(null);
		// loginConsort.restart();
		// }));
		// }
		// }
		// });
	}

	@Override
	protected String getEnteredText() {
		input.sync();
		return input.getValue();
	}

	@Override
	protected String getSubtitleText() {
		return loginConsort.getPasswordPageSubtitleText();
	}

	@Override
	protected Validator getValidator() {
		return new NotNullValidator();
	}

	@Override
	protected void onForwardValidated() {
		loginConsort.request.setPassword(getEnteredText());
		super.onForwardValidated();
	}
}
