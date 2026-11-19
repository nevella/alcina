package cc.alcina.framework.gwt.client.dirndl.impl.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.gwittir.widget.DateBox;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class FmsUtil {
	public static Widget cleanupStyles(Widget widget) {
		// FIXME - dirndl 1x2 - remove - remove ol- styles
		if (!widget.getStyleName().matches("ol-.*")) {
			widget.setStyleName("");
		}
		Element element = widget.getElement();
		if (element.getTagName().equals("select")) {
			FlowPanel wrapper = new FlowPanel();
			wrapper.add(widget);
			widget = wrapper;
		}
		if (widget instanceof DateBox) {
			DateBox dateBox = (DateBox) widget;
			dateBox.getPicker().addAttachHandler(evt -> {
				PopupPanel ancestorWidget = WidgetUtils.getAncestorWidget(
						dateBox.getPicker(), PopupPanel.class);
				ancestorWidget.setStyleName(
						"datepicker datepicker-dropdown dropdown-menu datepicker-orient-left datepicker-orient-bottom");
				ancestorWidget.getElement().getStyle()
						.setDisplay(Style.Display.BLOCK);
			});
			FlowPanel wrapper = new FlowPanel();
			wrapper.add(widget);
			widget = wrapper;
		}
		return widget;
	}
}
