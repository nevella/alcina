package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortSignalHandler;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.logic.CallManager;

public class LogoutWithReloadSignalHandler
		implements ConsortSignalHandler<HandshakeSignal>, AsyncCallback {
	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.LOGGED_OUT;
	}

	@Override
	public void onFailure(Throwable caught) {
		CallManager.get().completed(this);
		throw new WrappedRuntimeException(caught);
	}

	@Override
	public void onSuccess(Object result) {
		CallManager.get().completed(this);
		Window.Location.reload();
	}

	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		ClientBase.getCommonRemoteServiceAsyncInstance().logout(this);
		CallManager.get().register(this, "Logging out");
	}
}