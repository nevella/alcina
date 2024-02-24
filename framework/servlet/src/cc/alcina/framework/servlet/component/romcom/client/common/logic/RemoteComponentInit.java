package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentInit implements NativePreviewHandler {
	public void init() {
		Event.addNativePreviewHandler(this);
		History.addValueChangeHandler(hash -> {
			ClientRpc.send(Message.Mutations.ofLocation());
		});
		ClientRpc.session = ReflectiveSerializer
				.deserialize(ClientUtils.wndString(
						RemoteComponentProtocolServer.ROMCOM_SERIALIZED_SESSION_KEY));
		ClientRpc.send(Message.Startup.forClient());
	}

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		ProtocolMessageHandlerClient.dispatchEventMessage(
				(Event) event.getNativeEvent(), null, true);
	}
}
