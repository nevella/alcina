package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.lux.LuxButton;
import cc.alcina.framework.gwt.client.lux.LuxButtonPanel;
import cc.alcina.framework.gwt.client.lux.LuxContainer;
import cc.alcina.framework.gwt.client.lux.LuxModalPanel;
import cc.alcina.framework.gwt.client.lux.LuxStatusPanel;
import cc.alcina.framework.gwt.client.lux.LuxStyle.LuxStyleHead;
import cc.alcina.framework.gwt.client.lux.LuxStyle.LuxStyleModal;

public abstract class LoginPage extends LuxModalPanel {
	protected LoginConsort controller;

	public LoginPage(LoginConsort loginConsort) {
		super();
		this.controller = loginConsort;
		LoginStyles.LOGIN_PAGE.addTo(this);
	}

	@Override
	protected LuxButtonPanel createButtonsPanel() {
		LuxButtonPanel buttons = new LuxButtonPanel();
		defaultButton = new LuxButton().withText("Next")
				.withHandler(this::handleNext)
				.withAsyncTopic(controller.topicCallingRemote);
		buttons.addActionButton(defaultButton);
		return buttons;
	}

	@Override
	protected Widget createHeaderPanel() {
		LuxContainer head = new LuxContainer(LuxStyleModal.HEAD);
		Widget logo = controller.getLogo();
		if (logo != null) {
			LuxStyleHead.LOGO.addTo(logo);
			head.add(logo);
		}
		head.addStyledTextBlock(LuxStyleHead.TITLE, controller.getTitleText());
		head.addStyledTextBlock(LuxStyleHead.SUBTITLE, getSubtitleText());
		return head;
	}

	@Override
	protected void createStatusPanel() {
		super.createStatusPanel();
		((LuxStatusPanel) statusPanel).connectToTopics(
				controller.topicCallingRemote, controller.topicMessage);
	}

	protected abstract String getSubtitleText();

	void handleNext(ClickEvent event) {
		if (!validate()) {
			return;
		}
		controller.onClickNext(event);
	}
}
