package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.data.place.ActionPlace;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionHandler;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

public interface ActionWidgetSupplier extends LooseActionHandler {
	public Widget getWidget(ActionPlace place);

	default void performAction() {
	}

	default void warningLabel(Panel container, String text) {
		container.add(
				UsefulWidgetFactory.styledLabel(text, "action-widget-warning"));
	}
}
