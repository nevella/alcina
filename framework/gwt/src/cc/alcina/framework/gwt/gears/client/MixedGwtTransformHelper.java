package cc.alcina.framework.gwt.gears.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class MixedGwtTransformHelper {
	private AlcinaRpcRequestBuilder builder;

	private LoadObjectsHolder holder;

	public LoadObjectsHolder getHolder() {
		return this.holder;
	}

	public void setHolder(LoadObjectsHolder holder) {
		this.holder = holder;
	}

	public AlcinaRpcRequestBuilder getBuilder() {
		return this.builder;
	}

	private boolean useMixedObjectLoadSequence;

	public boolean isUseMixedObjectLoadSequence() {
		return this.useMixedObjectLoadSequence;
	}

	public <T extends DomainModelHolder> T handleLoad(
			LoadObjectsHolder<T> holder, AlcinaRpcRequestBuilder builder) {
		this.holder = holder;
		if (this.holder.getDomainObjects() == null) {
			try {
				LocalTransformPersistence.get()
						.persistAndReparentClientLoadTransforms(this);
				useMixedObjectLoadSequence = true;
				return (T) getDomainLoader().getLoadObjectsHolder().getDomainObjects();
			} catch (MixedGwtLoadException e) {
				ClientLayerLocator.get().notifications().log(e.getMessage());
				e.printStackTrace();
				if (e.isWipeOffline()) {
					LocalTransformPersistence.get().clearPersistedClient(null);
				}
			}
		}
		return holder.getDomainObjects();
	}

	private SerializedDomainLoader getDomainLoader() {
		ClientHandshakeHelper handshakeHelper = ClientLayerLocator.get()
				.getClientHandshakeHelper();
		if (handshakeHelper instanceof ClientHandshakeHelperWithLocalPersistence) {
			ClientHandshakeHelperWithLocalPersistence withLocalPersistence = (ClientHandshakeHelperWithLocalPersistence) handshakeHelper;
			if (withLocalPersistence.supportsRpcPersistence()) {
				SerializedDomainLoader loader = withLocalPersistence
						.getSerializedDomainLoader();
				return loader;
			}
		}
		return null;
	}

	public LoadObjectsRequest prepareRequest(AlcinaRpcRequestBuilder builder) {
		this.builder = builder;
		builder.setRecordResult(true);
		LoadObjectsRequest request = new LoadObjectsRequest();
		SerializedDomainLoader loader = getDomainLoader();
		if (loader.loadSerializedTransformsForOnline()) {
			LoadObjectsHolder<DomainModelHolder> holder = loader
					.getLoadObjectsHolder();
			request.setLastTransformId(holder.getLastTransformId());
		}
		request.setTypeSignature(GWT.getPermutationStrongName());
		return request;
	}

	public void persistGwtObjectGraph() {
		LocalTransformPersistence.get().persistInitialRpcPayload(this);
	}

	public AsyncCallback<LoadObjectsHolder> prepareForReplay(
			AlcinaRpcRequestBuilder builder, String text) {
		builder.setResponsePayload(text);
		return new LoaderCallback();
	}

	class LoaderCallback implements AsyncCallback<LoadObjectsHolder> {
		public void onFailure(Throwable caught) {
			// Different RPC signatures
		}

		public void onSuccess(LoadObjectsHolder result) {
			holder = result;
		}
	}
}
