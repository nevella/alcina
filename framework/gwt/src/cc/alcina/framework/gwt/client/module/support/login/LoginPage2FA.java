package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.validator.CompositeValidationFeedback;

import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.ide.ContentViewSections.ContentViewSection;
import cc.alcina.framework.gwt.client.logic.RenderContext;
import cc.alcina.framework.gwt.client.lux.LuxButton;
import cc.alcina.framework.gwt.client.lux.LuxButtonPanel;
import cc.alcina.framework.gwt.client.lux.LuxContainer;
import cc.alcina.framework.gwt.client.lux.LuxFormCellRenderer;
import cc.alcina.framework.gwt.client.lux.LuxStyleValidationFeedback;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage2FAModel.Login2FAModelBinding;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

public class LoginPage2FA extends LoginPage {
	private LoginPage2FAModel model;

	private FlowPanel imageContainer;

	int errorCounter;

	public LoginPage2FA(LoginConsort loginConsort) {
		super(loginConsort);
		this.model = new LoginPage2FAModel(loginConsort.request);
		render();
	}

	private void loadImage() {
		imageContainer.clear();
		Image image = new Image(UriUtils.fromTrustedString(
				this.controller.lastResponse.getTwoFactorAuthQRCode()));
		image.getElement().getStyle().setVisibility(Visibility.VISIBLE);
		new LuxContainer(LoginStyles.TWO_FACTOR_IMAGE).add(image)
				.addTo(imageContainer);
	}

	@Override
	protected LuxButtonPanel createButtonsPanel() {
		LuxButtonPanel buttons = super.createButtonsPanel();
		if (!this.controller.shouldShowQrCode()) {
			buttons.addOptionalButton(new LuxButton().withText("Show QA code")
					.withHandler(e -> imageContainer.setVisible(true)));
		}
		return buttons;
	}

	@Override
	protected Widget createContentPanel() {
		try {
			RenderContext.get().push();
			RenderContext.get().setValidationFeedbackSupplier(fieldName -> {
				RelativePopupValidationFeedback feedback = new RelativePopupValidationFeedback(
						RelativePopupValidationFeedback.BOTTOM);
				return new CompositeValidationFeedback(feedback,
						new LuxStyleValidationFeedback("validation-error"));
			});
			FlowPanel flowPanel = new FlowPanel();
			imageContainer = new FlowPanel();
			flowPanel.add(imageContainer);
			loadImage();
			imageContainer.setVisible(this.controller.shouldShowQrCode());
			{
				ContentViewSections sectionsBuilder = createBuilder();
				ContentViewSection section = sectionsBuilder.section("");
				section.fields(
						Login2FAModelBinding.twoFactorAuthenticationCode);
				section.cellRenderer(new LuxFormCellRenderer());
				Widget table = sectionsBuilder.buildWidget(model);
				flowPanel.add(table);
			}
			return flowPanel;
		} finally {
			RenderContext.get().pop();
		}
	}

	@Override
	protected String getSubtitleText() {
		if (this.controller.shouldShowQrCode()) {
			return "Scan the barcode with your Authenticator app, and enter the authentication code";
		} else {
			return "Enter the code from the Authenticator app";
		}
	}
}
