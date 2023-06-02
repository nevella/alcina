package cc.alcina.extras.dev.component.remote.client.common.logic;

import com.google.gwt.user.client.History;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentInit {
	public void init() {
		History.addValueChangeHandler(hash -> {
			ClientRpc.send(ProtocolMessage.Mutations.ofLocation());
		});
		ClientRpc.send(ProtocolMessage.Startup.forClient());
	}
}
