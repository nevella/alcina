package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.AllStatesConsort;
import cc.alcina.framework.common.client.consort.ConsortPlayer.SubconsortSupport;
import cc.alcina.framework.common.client.consort.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectDataState;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence.DeltaApplicationFilters;

public class RetrieveLocalModelTransformDeltasPlayer
		extends RunnableAsyncCallbackPlayer<Object, LoadObjectDataState> {
	private RetrieveModelConsort retrieveModelConsort;

	public RetrieveLocalModelTransformDeltasPlayer() {
		addProvides(
				LoadObjectDataState.LOADED_DELTA_APPLICATIONS_FROM_LOCAL_STORAGE);
		addRequires(LoadObjectDataState.HELLO_OFFLINE);
		addRequires(LoadObjectDataState.SOLE_OPEN_TAB_CHECKED);
	}

	@Override
	public void onSuccess(Object result) {
		HandshakeConsortModel.get().setFromPersistenceDeltas(
				retrieveModelConsort.fromPersistenceDeltas());
		super.onSuccess(result);
	}

	@Override
	public void run() {
		retrieveModelConsort = new RetrieveModelConsort(this);
		new SubconsortSupport().maybeAttach(this, retrieveModelConsort, false);
	}

	enum Phase {
		GET_CLIENT_INSTANCE_ID_OF_DOMAIN_OBJECT_DELTA,
		GET_DELTAS_FOR_CLIENT_INSTANCE
	}

	class RetrieveModelConsort extends AllStatesConsort<Phase> {
		public RetrieveModelConsort(AsyncCallback callback) {
			super(Phase.class, callback);
		}

		Iterator<DomainModelDelta> fromPersistenceDeltas() {
			return (Iterator<DomainModelDelta>) lastCallbackResult;
		}

		@Override
		public void runPlayer(AllStatesPlayer player, Phase next) {
			switch (next) {
			case GET_CLIENT_INSTANCE_ID_OF_DOMAIN_OBJECT_DELTA:
				LocalTransformPersistence.get()
						.getClientInstanceIdOfDomainObjectDelta(player);
				break;
			case GET_DELTAS_FOR_CLIENT_INSTANCE:
				DeltaApplicationRecord record = CommonUtils.first(
						(Iterator<DeltaApplicationRecord>) lastCallbackResult);
				DeltaApplicationFilters filters = new DeltaApplicationFilters();
				filters.clientInstanceId = -1L;
				if (record != null) {
					filters.clientInstanceId = record.getClientInstanceId();
				}
				LocalTransformPersistence.get()
						.getDomainModelDeltaIterator(filters, player);
				break;
			}
		}
	}
}
