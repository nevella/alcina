package cc.alcina.framework.jvmclient.persistence;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.AsyncConfigConsortState;

public class InitJdbcPlayer extends
		RunnableAsyncCallbackPlayer<Void, AsyncConfigConsortState> implements
		ConsortPlayer {
	private PersistenceTransformSetupJdbcConsort subConsort;

	public InitJdbcPlayer(JdbcTransformPersistence transformPersistence) {
		this.subConsort = new PersistenceTransformSetupJdbcConsort(transformPersistence);
		addProvides(AsyncConfigConsortState.TRANSFORM_DB_INITIALISED);
	}

	@Override
	public void run() {
		new SubconsortSupport().run(consort, subConsort, this);
	}

	@Override
	public Consort getStateConsort() {
		return subConsort;
	}
}