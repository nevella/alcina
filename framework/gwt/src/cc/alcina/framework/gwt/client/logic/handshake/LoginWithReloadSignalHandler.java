package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortSignalHandler;

public class LoginWithReloadSignalHandler
		implements ConsortSignalHandler<HandshakeSignal> {
	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.LOGGED_IN;
	}

	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		Window.Location.reload();
	}
}