package cc.alcina.extras.dev.component.remote.client.common.logic;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentInit {
	public void init() {
		ClientRpc.send(ProtocolMessage.Startup.forClient());
	}
}
