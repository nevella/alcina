package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.gwt.persistence.client.RemoteLogPersister;
import cc.alcina.framework.gwt.persistence.client.WebDatabaseTransformPersistence;

public class StandardWebdbAysncServicesPlayer extends InitAysncServicesPlayer
		implements ConsortPlayer {
	private AsyncConfigConsort asyncConfigConsort;
	public StandardWebdbAysncServicesPlayer() {
		this(WebDatabaseTransformPersistence.ALCINA_TRANSFORM_PERSISTENCE);
	}
	public StandardWebdbAysncServicesPlayer(String transformDbName) {
		this.asyncConfigConsort = new AsyncConfigConsort();
		asyncConfigConsort.addPlayer(new InitWebDbPlayer(transformDbName));
		RemoteLogPersister remoteLogPersister = new RemoteLogPersister();
		asyncConfigConsort.addPlayer(new InitPropAndLogDbPlayer(
				transformDbName, remoteLogPersister));
		asyncConfigConsort.addEndpointPlayer();
	}

	@Override
	public void run() {
		new SubconsortSupport().run(consort, asyncConfigConsort, this);
	}

	@Override
	public Consort getStateConsort() {
		return asyncConfigConsort;
	}
}
