package cc.alcina.framework.jvmclient.persistence;

import java.sql.Connection;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.persistence.client.LogStore;
import cc.alcina.framework.gwt.persistence.client.PersistencePropAndLogInitState;
import cc.alcina.framework.gwt.persistence.client.KeyValueStore;
import cc.alcina.framework.gwt.persistence.client.RemoteLogPersister;

public class PersistencePropAndLogJdbcConsort extends
		Consort<PersistencePropAndLogInitState> {
	private RemoteLogPersister remoteLogPersister;

	private ObjectStoreJdbcImpl logImpl;

	public ObjectStoreJdbcImpl propImpl;

	private Connection conn;

	class Player_PRE_PROPERTY_IMPL
			extends
			EnumRunnableAsyncCallbackPlayer<Void, PersistencePropAndLogInitState> {
		public Player_PRE_PROPERTY_IMPL() {
			super(PersistencePropAndLogInitState.PRE_PROPERTY_IMPL);
		}

		@Override
		public void run() {
			propImpl = new ObjectStoreJdbcImpl(conn, "PropertyStore", this);
		}
	}

	class Player_POST_PROPERTY_IMPL extends
			EnumPlayer<PersistencePropAndLogInitState> {
		public Player_POST_PROPERTY_IMPL() {
			super(PersistencePropAndLogInitState.POST_PROPERTY_IMPL);
		}

		@Override
		public void run() {
			KeyValueStore.get().registerDelegate(propImpl);
		}
	}

	class Player_PRE_LOG_IMPL
			extends
			EnumRunnableAsyncCallbackPlayer<Void, PersistencePropAndLogInitState> {
		public Player_PRE_LOG_IMPL() {
			super(PersistencePropAndLogInitState.PRE_LOG_IMPL);
		}

		@Override
		public void run() {
			logImpl = new ObjectStoreJdbcImpl(conn, "LogStore", this);
		}
	}

	class Player_POST_LOG_IMPL
			extends
			EnumRunnableAsyncCallbackPlayer<Void, PersistencePropAndLogInitState> {
		public Player_POST_LOG_IMPL() {
			super(PersistencePropAndLogInitState.POST_LOG_IMPL);
		}

		@Override
		public void run() {
			LogStore.get().registerDelegate(logImpl);
			if (remoteLogPersister != null) {
				LogStore.get().setRemoteLogPersister(remoteLogPersister);
				remoteLogPersister.push();
			}
			wasPlayed();
		}
	}

	public PersistencePropAndLogJdbcConsort(Connection conn,
			RemoteLogPersister remoteLogPersister) {
		this.conn = conn;
		this.remoteLogPersister = remoteLogPersister;
		addPlayer(new Player_PRE_PROPERTY_IMPL());
		addPlayer(new Player_POST_PROPERTY_IMPL());
		addPlayer(new Player_PRE_LOG_IMPL());
		addPlayer(new Player_POST_LOG_IMPL());
		addEndpointPlayer();
	}
}
