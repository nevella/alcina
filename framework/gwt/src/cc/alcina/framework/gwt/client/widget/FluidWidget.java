package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public interface FluidWidget<T extends Widget> {
	default T withStyleName(String styleName) {
		asWidget().setStyleName(styleName);
		return (T) this;
	}

	default T addTo(ComplexPanel complexPanel) {
		complexPanel.add(asWidget());
		return (T) this;
	}

	default Widget asWidget() {
		return (Widget) this;
	}
}