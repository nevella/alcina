package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.EnumPlayer;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.DatabaseException;

public class PersistencePropAndLogWebDbConsort extends
		Consort<PersistencePropAndLogInitState> {
	String dbName;

	private RemoteLogPersister remoteLogPersister;

	private ObjectStoreWebDbImpl logImpl;

	public ObjectStoreWebDbImpl propImpl;

	class Player_PRE_PROPERTY_IMPL extends
			EnumRunnableAsyncCallbackPlayer<Void, PersistencePropAndLogInitState> {
		public Player_PRE_PROPERTY_IMPL() {
			super(PersistencePropAndLogInitState.PRE_PROPERTY_IMPL);
		}

		@Override
		public void run() {
			Database db;
			try {
				db = Database.openDatabase(dbName, "1.0", "Property store",
						5000000);
			} catch (DatabaseException e) {
				// no db access
				consort.finished();
				return;
			}
			propImpl = new ObjectStoreWebDbImpl(db,
					"PropertyStore", this);
		}
	}

	class Player_POST_PROPERTY_IMPL extends
			EnumPlayer<PersistencePropAndLogInitState> {
		public Player_POST_PROPERTY_IMPL() {
			super(PersistencePropAndLogInitState.POST_PROPERTY_IMPL);
		}

		@Override
		public void run() {
			PropertyStore.get().registerDelegate(propImpl);
			wasPlayed();
		}
	}

	class Player_PRE_LOG_IMPL extends
			EnumRunnableAsyncCallbackPlayer<Void, PersistencePropAndLogInitState> {
		public Player_PRE_LOG_IMPL() {
			super(PersistencePropAndLogInitState.PRE_LOG_IMPL);
		}

		@Override
		public void run() {
			Database db = Database.openDatabase(dbName, "1.0", "Log store",
					5000000);
			logImpl = new ObjectStoreWebDbImpl(db, "LogStore", this);
		}
	}

	class Player_POST_LOG_IMPL extends
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
		}
	}

	public PersistencePropAndLogWebDbConsort(String dbName,
			RemoteLogPersister remoteLogPersister) {
		this.dbName = dbName;
		this.remoteLogPersister = remoteLogPersister;
		addPlayer(new Player_PRE_PROPERTY_IMPL());
		addPlayer(new Player_POST_PROPERTY_IMPL());
		addPlayer(new Player_PRE_LOG_IMPL());
		addPlayer(new Player_POST_LOG_IMPL());
		addEndpointPlayer();
	}
	
}
