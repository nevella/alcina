package cc.alcina.framework.gwt.persistence.client;

import com.google.code.gwt.database.client.Database;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.AllStatesConsort;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

public class PersistenceTransformSetupWebDbConsort extends
		AllStatesConsort<PersistenceTransformSetupState> {
	String dbName;

	private ObjectStoreWebDbImpl deltaImpl;

	public PersistenceTransformSetupWebDbConsort(String dbName) {
		super(PersistenceTransformSetupState.class, null);
		this.dbName = dbName;
	}

	@Override
	public void runPlayer(AllStatesPlayer player,
			PersistenceTransformSetupState next) {
		switch (next) {
		case TRANSFORM_TABLE_READY:
			LocalTransformPersistence
					.registerLocalTransformPersistence(new WebDatabaseTransformPersistence(
							dbName));
			LocalTransformPersistence.get().init(
					new DTESerializationPolicy(),
					Registry.impl(CommitToStorageTransformListener.class),
					 player);
			break;
		case DELTA_OBJECT_STORE_READY:
			Database db = Database.openDatabase(dbName, "1.0", "Delta store",
					5000000);
			deltaImpl = new ObjectStoreWebDbImpl(db, "DeltaStore",
					 player);
			break;
		case DELTA_STORE_LINKED:
			DeltaStore.get().registerDelegate(deltaImpl);
			DeltaStore.get().refreshCache(player);
			break;
		}
	}
}
