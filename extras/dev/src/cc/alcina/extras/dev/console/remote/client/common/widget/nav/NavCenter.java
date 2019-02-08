package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;

import cc.alcina.extras.dev.console.remote.client.common.widget.nav.NavStyles.NavStylesCenter;

public class NavCenter extends Composite {
    private FlowPanel fp;

    public NavCenter() {
        this.fp = new FlowPanel();
        initWidget(fp);
        render();
    }

    private void render() {
        FlowPanel menu = new FlowPanel();
        NavStylesCenter.MENU.set(menu);
        InlineHTML menuButton = new InlineHTML(
                "<a href='#menu'><span class='d1'></span>"
                        + "<span class='d2'></span><span class='d3'></span></a>");
        NavStylesCenter.MENU_BUTTON.set(menuButton);
        menu.add(menuButton);
        fp.add(menu);
        FlowPanel action = new FlowPanel();
        action.add(new InlineHTML("&#160;"));
        InlineHTML actionButton = new InlineHTML("<div></div>");
        NavStylesCenter.ACTION_BUTTON.set(actionButton);
        action.add(actionButton);
        NavStylesCenter.ACTION.set(action);
        fp.add(action);
    }
}
