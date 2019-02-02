package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public class Widgets {
    public static Widgets with(Widget widget) {
        Widgets widgets = new Widgets();
        widgets.widget = widget;
        return widgets;
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
