package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.validator.CompositeValidationFeedback;

import cc.alcina.framework.common.client.csobjects.LoginResponseState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.directed.RenderContext;
import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.ide.ContentViewSections.ContentViewSection;
import cc.alcina.framework.gwt.client.lux.LuxButton;
import cc.alcina.framework.gwt.client.module.support.login.LoginPagePasswordModel.LoginPagePasswordModelBinding;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

public class LoginPagePassword extends LoginPage {
	private LoginPagePasswordModel model;

	//
	public LoginPagePassword(LoginConsort loginConsort) {
		super(loginConsort);
		this.model = new LoginPagePasswordModel(loginConsort.request);
		render();
		loginConsort.topicCallingRemote.add((k, calling) -> {
			if (!calling && isAttached()) {
				buttonsPanel.removeOptionalButtons();
				if (loginConsort.lastResponse.getStates()
						.contains(LoginResponseState.Invalid_credentials)
						|| loginConsort.lastResponse.getStates().contains(
								LoginResponseState.Password_incorrect)) {
					buttonsPanel.addOptionalButton(
							new LuxButton().withText("Back").withHandler(e -> {
								loginConsort.request.setPassword(null);
								loginConsort.restart();
							}));
				}
			}
		});
	}

	@Override
	protected Widget createContentPanel() {
		try {
			RenderContext.get().push();
			LoginFormUI loginFormUI = Registry.impl(LoginFormUI.class);
			RenderContext.get().setValidationFeedbackSupplier(fieldName -> {
				RelativePopupValidationFeedback feedback = new RelativePopupValidationFeedback(
						RelativePopupValidationFeedback.BOTTOM);
				return new CompositeValidationFeedback(feedback,
						loginFormUI.getValidationFeedback());
			});
			{
				ContentViewSections sectionsBuilder = createBuilder();
				ContentViewSection section = sectionsBuilder.section("");
				section.fields(LoginPagePasswordModelBinding.password);
				section.cellRenderer(loginFormUI.getRenderer());
				Widget table = sectionsBuilder.buildWidget(model);
				return table;
			}
		} finally {
			RenderContext.get().pop();
		}
	}

	@Override
	protected String getSubtitleText() {
		return "Please enter your password";
	}
}
