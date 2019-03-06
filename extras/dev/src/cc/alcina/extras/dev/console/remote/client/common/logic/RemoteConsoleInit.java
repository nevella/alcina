package cc.alcina.extras.dev.console.remote.client.common.logic;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;

import cc.alcina.extras.dev.console.remote.client.RemoteConsoleLayout;
import cc.alcina.extras.dev.console.remote.client.RemoteConsoleLayout.RemoteConsoleLayoutMessage;
import cc.alcina.extras.dev.console.remote.client.common.resources.RemoteConsoleModule;
import cc.alcina.extras.dev.console.remote.client.module.dev.DevModule;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest.RemoteConsoleRequestType;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleResponse;
import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsRegistryDelegateCreator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;
import cc.alcina.framework.gwt.client.logic.ClientUTCDateProvider;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class RemoteConsoleInit {
    public void init() {
        loadCss();
        addDevCssListener();
        if (GWT.isScript()) {
            ClientPropertyReflector
                    .setDelegateCreator(new JsRegistryDelegateCreator());
        }
        CommonUtils.setSupplier = () -> new LightSet();
        if (GWT.isScript()) {
            Registry.setDelegateCreator(new JsRegistryDelegateCreator());
        }
        JavascriptKeyableLookup.initJs();
        Registry.get().registerBootstrapServices(ClientReflector.get());
        Reflections.registerClassLookup(ClientReflector.get());
        Registry.registerSingleton(CurrentUtcDateProvider.class,
                new ClientUTCDateProvider());
        RemoteConsoleModule.get();
        RemoteConsoleRequest request = RemoteConsoleRequest.create();
        request.setType(RemoteConsoleRequestType.STARTUP);
        RemoteConsoleClientUtils.submitRequest(request,
                this::handleStartupResponse);
        // model = new JadexInitModel();
        // model.fromJson(Jx.wndString(JadexInitModel.JS_INTEROP_KEY));
        // Registry.registerSingleton(ClientNotifications.class,
        // new JadeNotificationsImpl());
    }

    private void addDevCssListener() {
        NativePreviewHandler handler = new NativePreviewHandler() {
            @Override
            public void onPreviewNativeEvent(NativePreviewEvent event) {
                NativeEvent nativeEvent = event.getNativeEvent();
                String type = nativeEvent.getType();
                boolean altKey = nativeEvent.getAltKey();
                boolean shiftKey = nativeEvent.getShiftKey();
                // Remote - use jade event sys
                int keyCode = nativeEvent.getKeyCode();
                int charCode = nativeEvent.getCharCode();
                if (nativeEvent != null && type.equals("keydown") && altKey
                        && shiftKey) {
                    if (keyCode == (int) 'Y') {
                        DevModule.callDevStyleUI();
                        event.cancel();
                        WidgetUtils.squelchCurrentEvent();
                    }
                    if (keyCode == (int) 'J') {
                        RemoteConsoleLayout.get()
                                .fire(RemoteConsoleLayoutMessage.FOCUS_COMMAND_BAR);
                        event.cancel();
                        WidgetUtils.squelchCurrentEvent();
                    }
                }
                if (nativeEvent != null && type.equals("keypress")) {
                    if (altKey && shiftKey && WidgetUtils.recentSquelch()) {
                        WidgetUtils.squelchCurrentEvent();
                    }
                }
            }
        };
        Event.addNativePreviewHandler(handler);
    }

    private void loadCss() {
        // TODO Auto-generated method stub
    }

    void handleStartupResponse(RemoteConsoleResponse response) {
        RemoteConsole.models().setStartupModel(response.getStartupModel());
        Window.setTitle(Ax.format("DevConsole - %s",
                response.getStartupModel().getAppName()));
    }
}
