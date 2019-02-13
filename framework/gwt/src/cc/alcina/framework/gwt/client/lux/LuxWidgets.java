package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public class LuxWidgets {
    public static LuxWidgets with(Widget widget) {
        LuxWidgets widgets = new LuxWidgets();
        widgets.widget = widget;
        return widgets;
    }

    private Widget widget;

    public void addTo(ComplexPanel complexPanel) {
        complexPanel.add(widget);
    }

    public LuxWidgets withChild(Widget child) {
        ((ComplexPanel) widget).add(child);
        return this;
    }

    public LuxWidgets withStyle(LuxStyleType style) {
        style.set(widget);
        return this;
    }
}
