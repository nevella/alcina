package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.consort.ConsortPlayer;
import cc.alcina.framework.common.client.consort.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.persistence.client.PersistenceTransformSetupWebDbConsort;

public class InitWebDbPlayer
		extends RunnableAsyncCallbackPlayer<Void, AsyncConfigConsortState>
		implements ConsortPlayer {
	private PersistenceTransformSetupWebDbConsort subConsort;

	public InitWebDbPlayer(String dbName) {
		this.subConsort = new PersistenceTransformSetupWebDbConsort(dbName);
		addProvides(AsyncConfigConsortState.TRANSFORM_DB_INITIALISED);
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