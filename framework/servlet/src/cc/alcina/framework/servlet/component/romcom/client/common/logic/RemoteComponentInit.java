package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Startup;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentInit implements NativePreviewHandler {
	public void init() {
		Event.addNativePreviewHandler(this);
		initWindowListeners();
		History.addValueChangeHandler(hash -> {
			if (!RemoteObjectModelComponentState.get().firingLocationMutation) {
				ClientRpc.send(Message.Mutations.ofLocation());
			}
		});
		ClientRpc.get().transportLayer.session = ReflectiveSerializer
				.deserializeRpc(ClientUtils.wndString(
						RemoteComponentProtocolServer.ROMCOM_SERIALIZED_SESSION_KEY));
		Startup startupMessage = Message.Startup.forClient();
		ClientRpc.send(startupMessage);
	}

	// FIXME - dirndl - this can just be sent via normal event creation (ehich
	// now handles window events)
	void onPageHideNativeEvent() {
		Event event = new Event(BrowserEvents.PAGEHIDE);
		ProtocolMessageHandlerClient.dispatchEventMessage(event, null, false);
	}

	final native void initWindowListeners() /*-{
		var _this=this;
		$wnd.onpagehide = $entry(function (evt){
			_this. @cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteComponentInit::onPageHideNativeEvent()();
		});
		}-*/;

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		ProtocolMessageHandlerClient.dispatchEventMessage(
				(Event) event.getNativeEvent(), null, true);
	}
}
