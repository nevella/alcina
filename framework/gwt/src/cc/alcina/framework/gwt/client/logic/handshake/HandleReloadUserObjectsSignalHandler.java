package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortSignalHandler;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;

public class HandleReloadUserObjectsSignalHandler implements
		ConsortSignalHandler<HandshakeSignal> {
	@Override
	public void signal(Consort consort) {
		List<ExtensibleEnum> statesToRemove = new ArrayList<ExtensibleEnum>(
				ExtensibleEnum.forClassAndTag(HandshakeState.class,
						HandshakeState.TAG_POST_OBJECT_DATA_LOAD));
		// actually, we have to invalidate the layout anyway - rely on those
		// history tokens...(alcina rox)
		// statesToRemove.remove(HandshakeState.MAIN_LAYOUT_INITIALISED);
		consort.removeStates(statesToRemove);
		consort.addIfNotMember(new ReloadObjectsPlayer());
		consort.nudge();
	}

	static class ReloadObjectsPlayer extends RunnablePlayer<HandshakeState> {
		public ReloadObjectsPlayer() {
			addRequires(HandshakeState.MAIN_LAYOUT_INITIALISED);
		}

		@Override
		public void run() {
			int i = 3;
		}
	}

	@Override
	public HandshakeSignal handlesSignal() {
		return HandshakeSignal.OBJECTS_INVALIDATED;
	}
}