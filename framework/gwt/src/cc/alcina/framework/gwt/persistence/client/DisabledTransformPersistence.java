package cc.alcina.framework.gwt.persistence.client;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class DisabledTransformPersistence extends LocalTransformPersistence {
	@Override
	public void clearPersistedClient(ClientInstance exceptFor,
			AsyncCallback callback) {
	}

	@Override
	protected void clearAllPersisted(AsyncCallback callback) {
	}

	@Override
	protected void getTransforms(DomainTransformRequestType[] types,
			AsyncCallback<List<DTRSimpleSerialWrapper>> callback) {
	}

	@Override
	protected void persist(DTRSimpleSerialWrapper wrapper,
			AsyncCallback callback) {
	}

	@Override
	public void reparentToClientInstance(DTRSimpleSerialWrapper wrapper,
			ClientInstance clientInstance, AsyncCallback callback) {
	}

	@Override
	protected void transformPersisted(
			List<DTRSimpleSerialWrapper> persistedWrappers,
			AsyncCallback callback) {
	}

	@Override
	public String getPersistenceStoreName() {
		return "Persistence disabled";
	}

	@Override
	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			AsyncCallback<Void> callback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				callback);
	}
}
