package cc.alcina.extras.dev.component.remote.client.common.logic;

import com.google.gwt.dom.client.Document;

import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest.RemoteComponentRequestType;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;

public class RemoteComponentInit {
	public void init() {
		RemoteComponentRequest request = RemoteComponentRequest.create();
		request.setType(RemoteComponentRequestType.STARTUP);
		RemoteComponentClientUtils.submitRequest(request,
				this::handleStartupResponse);
	}

	void handleStartupResponse(RemoteComponentResponse response) {
		Document.get().setTitle("bruce");
	}
}
