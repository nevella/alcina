package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.widget.Widgets;

public class LuxWidgets {
    public static LuxWidgets container() {
        return LuxWidgets.with(new LuxContainer());
    }

    public static LuxWidgets withText(String text) {
        return with(Widgets.unstyledText(text));
    }

    public static LuxWidgets with(Widget widget) {
        LuxWidgets widgets = new LuxWidgets();
        widgets.widget = widget;
        return widgets;
    }

    private Widget widget;

    private LuxWidgets() {
        // private constructor
    }

    public <W extends Widget> W addTo(ComplexPanel complexPanel) {
        complexPanel.add(widget);
        return (W) widget;
    }

    public <W extends Widget> W addTo(LuxContainer luxContainer) {
        luxContainer.add(widget);
        return (W) widget;
    }

    public <W extends Widget> W build() {
        return (W) widget;
    }

    public LuxWidgets withChild(Widget child) {
        if (widget instanceof LuxContainer) {
            ((LuxContainer) widget).add(child);
        } else {
            ((ComplexPanel) widget).add(child);
        }
        return this;
    }

    public LuxWidgets withChildText(String text) {
        Label child = Widgets.unstyledText(text);
        if (widget instanceof LuxContainer) {
            ((LuxContainer) widget).add(child);
        } else {
            ((ComplexPanel) widget).add(child);
        }
        return this;
    }

    public LuxWidgets withStyle(LuxStyleType style) {
        style.set(widget);
        return this;
    }

    public LuxWidgets withWrappedChild(Widget child) {
        return withChild(Widgets.wrapInSimplePanel(child));
    }
}
