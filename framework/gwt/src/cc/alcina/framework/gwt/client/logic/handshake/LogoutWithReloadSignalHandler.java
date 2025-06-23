package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.consort.ConsortSignalHandler;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.gwt.client.Client;
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
		// FIXME - romcom - better handle at another level (post-close mutations
		// should be silently dropped, not fail)
		if (Al.isBrowser()) {
			CallManager.get().completed(this);
		}
		Window.Location.reload();
	}

	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		Client.commonRemoteService().logout(this);
		// FIXME - romcom - better handle at another level (post-close mutations
		// should be silently dropped, not fail)
		if (Al.isBrowser()) {
			CallManager.get().register(this, "Logging out");
		}
	}
}