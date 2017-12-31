package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortSignalHandler;

public class HandleReloadUserObjectsSignalHandler
		implements ConsortSignalHandler<HandshakeSignal> {
	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.OBJECTS_INVALIDATED;
	}

	@Override
	public void signal(Consort consort, AsyncCallback signalHandledCallback) {
		consort.addOneTimeFinishedCallback(signalHandledCallback);
		List<ExtensibleEnum> statesToRemove = new ArrayList<ExtensibleEnum>(
				ExtensibleEnum.forClassAndTag(HandshakeState.class,
						HandshakeState.TAG_POST_OBJECT_DATA_LOAD));
		// actually, we have to invalidate the layout anyway - rely on those
		// history tokens...(alcina rox)
		// statesToRemove.remove(HandshakeState.MAIN_LAYOUT_INITIALISED);
		consort.removeStates(statesToRemove);
		consort.addIfNotMember(Registry.impl(StartAppPlayer.class));
		consort.nudge();
	}
}