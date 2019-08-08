package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsole;
import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleModels;

public class NavComponent extends Composite {
    private FlowPanel fp;

    public NavComponent() {
        this.fp = new FlowPanel();
        NavModule.get();
        initWidget(fp);
        NavStyles.NAV_MODULE.set(this);
        render();
    }

    private void render() {
        FlowPanel bar = new FlowPanel();
        fp.add(bar);
        NavStyles.BAR.set(bar);
        FlowPanel logo = new FlowPanel();
        NavStyles.LOGO.set(logo);
        InlineLabel appNameLabel = new InlineLabel("Remote Console");
        logo.add(appNameLabel);
        RemoteConsoleModels.topicStartupModelLoaded()
                .addRunnable(() -> appNameLabel.setText(
                        RemoteConsole.models().getStartupModel().getAppName()),
                        true);
        bar.add(logo);
    }
}
