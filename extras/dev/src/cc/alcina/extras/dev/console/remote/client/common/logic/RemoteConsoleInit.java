package cc.alcina.extras.dev.console.remote.client.common.logic;

import java.util.Collections;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

import cc.alcina.extras.dev.console.remote.client.RemoteConsoleLayout;
import cc.alcina.extras.dev.console.remote.client.RemoteConsoleLayout.RemoteConsoleLayoutMessage;
import cc.alcina.extras.dev.console.remote.client.common.resources.RemoteConsoleModule;
import cc.alcina.extras.dev.console.remote.client.module.dev.DevModule;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest.RemoteConsoleRequestType;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleResponse;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializerC;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl;
import cc.alcina.framework.gwt.client.util.TimerGwt;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class RemoteConsoleInit {
	private void addDevCssListener() {
		NativePreviewHandler handler = new NativePreviewHandler() {
			@Override
			public void onPreviewNativeEvent(NativePreviewEvent event) {
				NativeEvent nativeEvent = event.getNativeEvent();
				String type = nativeEvent.getType();
				switch (type) {
				case "keydown":
				case "keypress":
					break;
				default:
					return;
				}
				boolean altKey = nativeEvent.getAltKey();
				boolean shiftKey = nativeEvent.getShiftKey();
				int keyCode = nativeEvent.getKeyCode();
				int charCode = nativeEvent.getCharCode();
				if (type.equals("keydown") && altKey && shiftKey) {
					if (keyCode == (int) 'Y') {
						DevModule.callDevStyleUI();
						event.cancel();
						WidgetUtils.squelchCurrentEvent();
					}
					if (keyCode == (int) 'J') {
						RemoteConsoleLayout.get().topicLayoutMessage.publish(
								RemoteConsoleLayoutMessage.FOCUS_COMMAND_BAR);
						event.cancel();
						WidgetUtils.squelchCurrentEvent();
					}
				}
				if (type.equals("keypress")) {
					if (altKey && shiftKey && WidgetUtils.recentSquelch()) {
						WidgetUtils.squelchCurrentEvent();
					}
				}
			}
		};
		Event.addNativePreviewHandler(handler);
	}

	void handleStartupResponse(RemoteConsoleResponse response) {
		RemoteConsoleClientImpl.models()
				.setStartupModel(response.getStartupModel());
		Document.get().setTitle(Ax.format("DevConsole - %s",
				response.getStartupModel().getAppName()));
	}

	public void init() {
		Registry.register().singleton(Timer.Provider.class,
				new TimerGwt.Provider());
		Registry.register().singleton(ClientNotifications.class,
				new ClientNotificationsImpl());
		Registry.register().add(AlcinaBeanSerializerC.class.getName(),
				Collections.singletonList(AlcinaBeanSerializer.class.getName()),
				Registration.Implementation.INSTANCE,
				Registration.Priority.APP);
		loadCss();
		addDevCssListener();
		RemoteConsoleModule.get();
		RemoteConsoleRequest request = RemoteConsoleRequest.create();
		request.setType(RemoteConsoleRequestType.STARTUP);
		RemoteConsoleClientUtils.submitRequest(request,
				this::handleStartupResponse);
	}

	private void loadCss() {
	}
}
