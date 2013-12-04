package cc.alcina.framework.jvmclient.persistence;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.AsyncConfigConsort;
import cc.alcina.framework.gwt.client.logic.handshake.InitAysncServicesPlayer;
import cc.alcina.framework.gwt.persistence.client.RemoteLogPersister;

public class StandardJdbcAysncServicesPlayer extends InitAysncServicesPlayer
		implements ConsortPlayer {
	private AsyncConfigConsort asyncConfigConsort;

	public StandardJdbcAysncServicesPlayer() {
		this(null);
	}

	public StandardJdbcAysncServicesPlayer(
			JdbcTransformPersistence transformPersistence) {
		this.asyncConfigConsort = new AsyncConfigConsort();
		asyncConfigConsort.addPlayer(new InitJdbcPlayer(transformPersistence));
		RemoteLogPersister remoteLogPersister = new RemoteLogPersister();
		asyncConfigConsort.addPlayer(new InitPropAndLogJdbcPlayer(
				transformPersistence.getConnection(), remoteLogPersister));
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
