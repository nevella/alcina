package cc.alcina.extras.dev.console.remote.client.module.dev;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.TextArea;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleClientUtils;
import cc.alcina.framework.gwt.client.lux.LuxModule;

public class DevModule {
    public static void callDevStyleUI() {
        GWT.runAsync(DevModule.class, RemoteConsoleClientUtils
                .runAsyncCallback(() -> DevModule.callDevStyleUIAsync()));
    }

    private static void callDevStyleUIAsync() {
        DialogBox dialogBox = new DialogBox();
        TextArea textArea = new TextArea();
        dialogBox.add(textArea);
        dialogBox.setAutoHideEnabled(true);
        dialogBox.show();
        dialogBox.addCloseHandler(evt -> LuxModule.get()
                .interpolateAndInject(textArea.getText()));
        textArea.addChangeHandler(evt -> dialogBox.hide());
        textArea.setVisibleLines(10);
        textArea.setCharacterWidth(30);
        dialogBox.center();
        dialogBox.show();
        textArea.setFocus(true);
    }
}
