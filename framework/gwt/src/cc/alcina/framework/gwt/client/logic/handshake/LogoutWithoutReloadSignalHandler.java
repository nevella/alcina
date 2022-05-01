package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.consort.ConsortSignalHandler;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class LogoutWithoutReloadSignalHandler
		implements ConsortSignalHandler<HandshakeSignal> {
	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.LOGGED_OUT;
	}

	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		consort.removeStates(ExtensibleEnum.forClassAndTag(HandshakeState.class,
				HandshakeState.TAG_POST_OBJECT_DATA_LOAD));
		consort.addIfNotMember(new StartAppPlayer());
		consort.nudge();
	}
}