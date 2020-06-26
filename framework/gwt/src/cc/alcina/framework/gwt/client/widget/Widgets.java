package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class Widgets {
	public static Label unstyledText(String text) {
		Label label = new Label(text);
		label.setStyleName("");
		return label;
	}

	public static Widgets with(Widget widget) {
		Widgets widgets = new Widgets();
		widgets.widget = widget;
		return widgets;
	}

	public static SimplePanel wrapInSimplePanel(Widget child) {
		SimplePanel panel = new SimplePanel(child);
		return panel;
	}

	private Widget widget;

	public void addTo(ComplexPanel complexPanel) {
		complexPanel.add(widget);
	}

	public Widgets className(String className) {
		widget.addStyleName(className);
		return this;
	}
}
