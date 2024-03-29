package cc.alcina.framework.jvmclient.persistence;

import cc.alcina.framework.common.client.consort.AllStatesConsort;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.gwt.persistence.client.DeltaStore;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.PersistenceTransformSetupState;

public class PersistenceTransformSetupJdbcConsort
		extends AllStatesConsort<PersistenceTransformSetupState> {
	private ObjectStoreJdbcImpl deltaImpl;

	private JdbcTransformPersistence transformPersistence;

	public PersistenceTransformSetupJdbcConsort(
			JdbcTransformPersistence transformPersistence) {
		super(PersistenceTransformSetupState.class, null);
		this.transformPersistence = transformPersistence;
	}

	@Override
	public void runPlayer(AllStatesPlayer player,
			PersistenceTransformSetupState next) {
		switch (next) {
		case TRANSFORM_TABLE_READY:
			LocalTransformPersistence
					.registerLocalTransformPersistence(transformPersistence);
			LocalTransformPersistence.get().init(new DTESerializationPolicy(),
					Registry.impl(CommitToStorageTransformListener.class),
					player);
			break;
		case DELTA_OBJECT_STORE_READY:
			deltaImpl = new ObjectStoreJdbcImpl(
					transformPersistence.getConnection(), "DeltaStore", player);
			break;
		case DELTA_STORE_LINKED:
			DeltaStore.get().registerDelegate(deltaImpl);
			DeltaStore.get().refreshCache(player);
			break;
		}
	}
}
