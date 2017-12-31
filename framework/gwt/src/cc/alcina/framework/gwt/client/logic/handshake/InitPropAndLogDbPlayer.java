package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.persistence.client.PersistencePropAndLogWebDbConsort;
import cc.alcina.framework.gwt.persistence.client.RemoteLogPersister;

public class InitPropAndLogDbPlayer
		extends RunnableAsyncCallbackPlayer<Void, AsyncConfigConsortState>
		implements ConsortPlayer {
	private PersistencePropAndLogWebDbConsort subConsort;

	public InitPropAndLogDbPlayer(String dbName,
			RemoteLogPersister remoteLogPersister) {
		this.subConsort = new PersistencePropAndLogWebDbConsort(dbName,
				remoteLogPersister);
		addRequires(AsyncConfigConsortState.TRANSFORM_DB_INITIALISED);
		addProvides(AsyncConfigConsortState.LOG_DB_INITIALISED);
	}

	@Override
	public Consort getStateConsort() {
		return subConsort;
	}

	@Override
	public void run() {
		new SubconsortSupport().run(consort, subConsort, this);
	}
}