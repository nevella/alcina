package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentInit {
	public void init() {
		History.addValueChangeHandler(hash -> {
			ClientRpc.send(Message.Mutations.ofLocation());
		});
		ClientRpc.session = ReflectiveSerializer
				.deserialize(ClientUtils.wndString(
						RemoteComponentProtocolServer.ROMCOM_SERIALIZED_SESSION_KEY));
		ClientRpc.send(Message.Startup.forClient());
	}
}
