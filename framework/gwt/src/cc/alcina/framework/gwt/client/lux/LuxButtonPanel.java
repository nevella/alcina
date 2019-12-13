package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LuxButtonPanel extends Composite {
	private LuxContainer panel;

	private LuxContainer optional;

	private LuxContainer actions;

	public LuxButtonPanel() {
		this.panel = LuxWidgets.container()
				.withStyle(LuxButtonStyle.LUX_BUTTON_PANEL).build();
		initWidget(panel);
		this.optional = LuxWidgets.container()
				.withStyle(LuxButtonStyle.OPTIONAL).addTo(panel);
		this.actions = LuxWidgets.container().withStyle(LuxButtonStyle.ACTIONS)
				.addTo(panel);
	}

	public void addActionButton(Widget button) {
		LuxButtonStyle.LUX_BUTTON.add(button);
		actions.add(button);
	}

	public void addOptionalButton(Widget button) {
		LuxButtonStyle.LUX_BUTTON.add(button);
		LuxButtonStyle.OPTIONAL_BUTTON.add(button);
		optional.add(button);
	}

	public static class Builder {
	}
}
