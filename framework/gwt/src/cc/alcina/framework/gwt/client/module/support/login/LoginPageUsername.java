package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.validator.CompositeValidationFeedback;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.directed.RenderContext;
import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.ide.ContentViewSections.ContentViewSection;
import cc.alcina.framework.gwt.client.module.support.login.LoginPageUsernameModel.LoginPageUsernameModelBinding;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

public class LoginPageUsername extends LoginPage {
	private LoginPageUsernameModel model;

	public LoginPageUsername(LoginConsort loginConsort) {
		super(loginConsort);
		this.model = new LoginPageUsernameModel(loginConsort.request);
		render();
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
				section.fields(LoginPageUsernameModelBinding.userName);
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
		return "Enter your email to log in";
	}
}
