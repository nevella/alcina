package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientMetricLogging;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

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

	public <T extends DomainModelHolder> void handleLoad(
			LoadObjectsHolder<T> holder, AlcinaRpcRequestBuilder builder,
			final PersistenceCallback<T> onLoadCallback) {
		this.holder = holder;
		if (this.holder.getDomainObjects() == null) {
			PersistenceCallback<Void> postPersistCallback = new PersistenceCallback<Void>() {
				@Override
				public void onFailure(Throwable e) {
					cleanup();
					ClientLayerLocator.get().notifications()
							.log(e.getMessage());
					if (PersistenceExceptionInterceptor.get()
							.checkTerminateAfterPossiblePersistenceException(e)) {
						return;
					}
					e.printStackTrace();
					MixedGwtLoadException ex = CommonUtils.extractCauseOfClass(
							e, MixedGwtLoadException.class);
					if (ex != null && ex.isWipeOffline()) {
						LocalTransformPersistence.get().clearPersistedClient(
								null, PersistenceCallback.VOID_CALLBACK);
					}
					onLoadCallback.onSuccess((T) getDomainLoader()
							.getLoadObjectsHolder().getDomainObjects());
				}

				@Override
				public void onSuccess(Void result) {
					cleanup();
					useMixedObjectLoadSequence = true;
					onLoadCallback.onSuccess((T) getDomainLoader()
							.getLoadObjectsHolder().getDomainObjects());
				}

				void cleanup() {
					ClientMetricLogging.get().end("persist-rpc-transforms");
				}
			};
			ClientMetricLogging.get().start("persist-rpc-transforms");
			LocalTransformPersistence.get()
					.persistAndReparentClientLoadTransforms(this,
							postPersistCallback);
			return;
		}
		onLoadCallback.onSuccess(holder.getDomainObjects());
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

	public void prepareRequest(AlcinaRpcRequestBuilder builder,
			final PersistenceCallback<LoadObjectsRequest> persistenceCallback) {
		this.builder = builder;
		builder.setRecordResult(true);
		final LoadObjectsRequest request = new LoadObjectsRequest();
		final SerializedDomainLoader loader = getDomainLoader();
		PersistenceCallback<Boolean> loadSerializedTransformsHandler = new PersistenceCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Boolean result) {
				if (result) {
					LoadObjectsHolder<DomainModelHolder> holder = loader
							.getLoadObjectsHolder();
					request.setLastTransformId(holder.getLastTransformId());
				}
				request.setTypeSignature(GWT.getPermutationStrongName());
				persistenceCallback.onSuccess(request);
			}
		};
		loader.loadSerializedTransformsForOnline(loadSerializedTransformsHandler);
	}

	public void persistGwtObjectGraph(PersistenceCallback<Void> callback) {
		LocalTransformPersistence.get()
				.persistInitialRpcPayload(this, callback);
	}

	public AsyncCallback<LoadObjectsHolder> prepareForReplay(
			AlcinaRpcRequestBuilder builder, String text,
			AsyncCallback<LoadObjectsHolder> onLoadCallback) {
		builder.setResponsePayload(text);
		return new LoaderCallback(onLoadCallback);
	}

	class LoaderCallback implements AsyncCallback<LoadObjectsHolder> {
		private final AsyncCallback<LoadObjectsHolder> onLoadCallback;

		public LoaderCallback(AsyncCallback<LoadObjectsHolder> onLoadCallback) {
			this.onLoadCallback = onLoadCallback;
		}

		public void onFailure(Throwable caught) {
			// Different RPC signatures
			onLoadCallback.onFailure(caught);
		}

		public void onSuccess(LoadObjectsHolder result) {
			holder = result;
			onLoadCallback.onSuccess(result);
		}
	}
}
