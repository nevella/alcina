package cc.alcina.framework.gwt.persistence.client;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

public class DisabledTransformPersistence extends LocalTransformPersistence {
	@Override
	public void clearPersistedClient(ClientInstance exceptFor,
			PersistenceCallback callback) {
	}

	@Override
	protected void clearAllPersisted(PersistenceCallback callback) {
	}

	@Override
	protected void getTransforms(DomainTransformRequestType[] types,
			PersistenceCallback<List<DTRSimpleSerialWrapper>> callback) {
	}

	@Override
	protected void persist(DTRSimpleSerialWrapper wrapper,
			PersistenceCallback callback) {
	}

	@Override
	public void reparentToClientInstance(DTRSimpleSerialWrapper wrapper,
			ClientInstance clientInstance, PersistenceCallback callback) {
	}

	@Override
	protected void transformPersisted(
			List<DTRSimpleSerialWrapper> persistedWrappers,
			PersistenceCallback callback) {
	}

	@Override
	public String getPersistenceStoreName() {
		return "Persistence disabled";
	}

	@Override
	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			PersistenceCallback<Void> callback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				callback);
	}
}
