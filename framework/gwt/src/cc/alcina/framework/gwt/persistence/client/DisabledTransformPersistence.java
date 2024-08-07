package cc.alcina.framework.gwt.persistence.client;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

public class DisabledTransformPersistence extends LocalTransformPersistence {
	@Override
	protected void clearAllPersisted(AsyncCallback callback) {
	}

	@Override
	public void clearPersistedClient(ClientInstance exceptFor, int exceptForId,
			AsyncCallback callback, boolean clearDeltaStore) {
	}

	@Override
	public void getClientInstanceIdOfDomainObjectDelta(AsyncCallback callback) {
	}

	@Override
	public void getDomainModelDeltaIterator(DeltaApplicationFilters filters,
			AsyncCallback<Iterator<DomainModelDelta>> callback) {
	}

	@Override
	public String getPersistenceStoreName() {
		return "Persistence disabled";
	}

	@Override
	protected void getTransforms(DeltaApplicationFilters filters,
			AsyncCallback<Iterator<DeltaApplicationRecord>> callback) {
	}

	@Override
	protected void getTransforms(DeltaApplicationRecordType[] types,
			AsyncCallback<Iterator<DeltaApplicationRecord>> callback) {
	}

	@Override
	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			AsyncCallback<Void> callback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				callback);
	}

	@Override
	protected void persistFromFrontOfQueue(DeltaApplicationRecord wrapper,
			AsyncCallback callback) {
	}

	@Override
	public void reparentToClientInstance(DeltaApplicationRecord wrapper,
			ClientInstance clientInstance, AsyncCallback callback) {
	}

	@Override
	protected void transformPersisted(
			List<DeltaApplicationRecord> persistedWrappers,
			AsyncCallback callback) {
	}
}
