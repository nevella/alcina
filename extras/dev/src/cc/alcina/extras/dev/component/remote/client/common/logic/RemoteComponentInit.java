package cc.alcina.extras.dev.component.remote.client.common.logic;

import com.google.gwt.dom.client.Document;

import cc.alcina.extras.dev.component.remote.protocol.ProtocolMessage;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.framework.common.client.util.Ax;

public class RemoteComponentInit {
	public void init() {
		RemoteComponentRequest request = RemoteComponentRequest.create();
		request.protocolMessage = ProtocolMessage.Startup.forClient();
		RemoteComponentClientUtils.submitRequest(request,
				this::handleStartupResponse);
	}

	void handleStartupResponse(RemoteComponentResponse response) {
		Document.get().setTitle("bruce");
		Ax.out("bruce -- ");
	}
}
