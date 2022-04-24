package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.consort.ConsortSignalHandler;
import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class HandleLoggedInSignalHandler
		implements ConsortSignalHandler<HandshakeSignal> {
	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.LOGGED_IN;
	}

	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		consort.removeStates(ExtensibleEnum.forClassAndTag(HandshakeState.class,
				HandshakeState.TAG_POST_OBJECT_DATA_LOAD));
		// don't clear existing objects, leave that for application logic
		consort.addIfNotMember(Registry.impl(StartAppPlayer.class));
		consort.nudge();
	}
}