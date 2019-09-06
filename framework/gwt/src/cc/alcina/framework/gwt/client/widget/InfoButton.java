package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class InfoButton extends Composite {
    public InfoButton(String string) {
        InlineHTML w = new InlineHTML("i");
        initWidget(w);
        setStyleName("info-button");
        w.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Widget sender = (Widget) event.getSource();
                final PopupPanel p = new PopupPanel(true);
                p.setStyleName("info-button-panel");
                p.setWidget(new HTML(string));
                p.setPopupPosition(sender.getAbsoluteLeft(),
                        sender.getAbsoluteTop() + sender.getOffsetHeight());
                p.getElement().getStyle().setZIndex(999);
                p.show();
            }
        });
    }
}
