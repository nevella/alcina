package cc.alcina.framework.gwt.client.lux;

import cc.alcina.framework.gwt.client.lux.LuxMainPanel;
import cc.alcina.framework.gwt.client.lux.LuxStyles;

public class LuxMainPanelBuilder {
	private String header = null;

	public LuxMainPanel build() {
		LuxMainPanel panel = new LuxMainPanel();
		LuxStyles.MAIN_PANEL.set(panel);
		if (header != null) {
			panel.addHeader(1, header);
		}
		return panel;
	}

	public LuxMainPanelBuilder header(String header) {
		this.header = header;
		return this;
	}
}
