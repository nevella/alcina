package cc.alcina.framework.jvmclient.persistence;

import java.sql.Connection;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.AsyncConfigConsortState;
import cc.alcina.framework.gwt.persistence.client.RemoteLogPersister;

public class InitPropAndLogJdbcPlayer
		extends RunnableAsyncCallbackPlayer<Void, AsyncConfigConsortState>
		implements ConsortPlayer {
	private PersistencePropAndLogJdbcConsort subConsort;

	public InitPropAndLogJdbcPlayer(Connection connection,
			RemoteLogPersister remoteLogPersister) {
		this.subConsort = new PersistencePropAndLogJdbcConsort(connection,
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