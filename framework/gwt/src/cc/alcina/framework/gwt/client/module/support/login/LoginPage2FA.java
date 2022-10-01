package cc.alcina.framework.gwt.client.module.support.login;

import cc.alcina.framework.common.client.gwittir.validator.CompositeValidator;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Editable;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class LoginPage2FA extends LoginPage {
	public LoginPage2FA(LoginConsort loginConsort) {
		super(loginConsort);
		setContents(new UiModel(loginConsort));
	}

	@Override
	protected String getSubtitleText() {
		if (this.loginConsort.shouldShowQrCode()) {
			return "Scan the barcode with your Authenticator app, and enter the authentication code";
		} else {
			return "Enter the code from the Authenticator app";
		}
	}
	// @Override
	// protected LuxButtonPanel createButtonsPanel() {
	// LuxButtonPanel buttons = super.createButtonsPanel();
	// if (!this.controller.shouldShowQrCode()) {
	// buttons.addOptionalButton(new LuxButton().withText("Show QA code")
	// .withHandler(e -> imageContainer.setVisible(true)));
	// }
	// return buttons;
	// }

	@Override
	protected String getEnteredText() {
		return ((UiModel) getContents()).input.getValue();
	}

	@Override
	protected CompositeValidator getValidator() {
		return null;
	}

	@Directed
	public static class UiModel extends Model {
		private final LeafRenderer.Image image;

		private final Editable.StringInput input;

		public UiModel(LoginConsort loginConsort) {
			input = new Editable.StringInput();
			input.setFocusOnAttach(true);
			input.setPlaceholder("2FA code");
			image = new LeafRenderer.Image(
					loginConsort.getLastResponse().getTwoFactorAuthQRCode());
		}

		@Directed
		public LeafRenderer.Image getImage() {
			return this.image;
		}

		@Directed
		public Editable.StringInput getInput() {
			return this.input;
		}
	}
}
