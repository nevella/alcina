package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.entity.place.ActionPlace;
import cc.alcina.framework.gwt.client.ide.provider.ActionPlaceHandler;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

public interface ActionWidgetSupplier<P extends ActionPlace>
		extends ActionPlaceHandler<P> {
	public Widget getWidget(P place);

	@Override
	default void performAction() {
	}

	default void warningLabel(Panel container, String text) {
		container.add(
				UsefulWidgetFactory.styledLabel(text, "action-widget-warning"));
	}
}
