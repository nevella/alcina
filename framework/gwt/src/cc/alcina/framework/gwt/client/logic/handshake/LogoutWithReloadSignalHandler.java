package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortSignalHandler;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CallManager;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class LogoutWithReloadSignalHandler implements
		ConsortSignalHandler<HandshakeSignal>, AsyncCallback {
	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		ClientLayerLocator.get().commonRemoteServiceAsyncInstance()
				.logout(this);
		CallManager.get().register(this, "Logging out");
	}

	@Override
	public void onSuccess(Object result) {
		CallManager.get().completed(this);
		Window.Location.reload();
	}

	@Override
	public void onFailure(Throwable caught) {
		CallManager.get().completed(this);
		throw new WrappedRuntimeException(caught);
	}

	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.LOGGED_OUT;
	}
}