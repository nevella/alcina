package cc.alcina.framework.gwt.client.logic;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class PossiblePanelProvider {
	public ComplexPanel providePanel() {
		RootPanel.get().clear();
		FlowPanel fp = new FlowPanel();
		RootPanel.get().add(fp);
		return fp;
	}
}
