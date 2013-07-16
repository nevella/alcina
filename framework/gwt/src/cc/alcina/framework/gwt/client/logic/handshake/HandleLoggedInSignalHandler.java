package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortSignalHandler;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class HandleLoggedInSignalHandler implements
		ConsortSignalHandler<HandshakeSignal> {
	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		consort.removeStates(ExtensibleEnum.forClassAndTag(
				HandshakeState.class, HandshakeState.TAG_POST_OBJECT_DATA_LOAD));
		// don't clear existing objects, leave that for application logic
		consort.addIfNotMember(new StartAppPlayer());
		consort.nudge();
	}

	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.LOGGED_IN;
	}
}