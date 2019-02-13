package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Widget;

public class LuxMainPanelBuilder {
    private String header = null;

    private LuxMainPanel panel;

    public LuxMainPanelBuilder() {
        panel = new LuxMainPanel();
        LuxStyle.MAIN_PANEL.set(panel);
    }

    public LuxMainPanelBuilder add(Widget widget) {
        panel.add(widget);
        return this;
    }

    public LuxMainPanel build() {
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
