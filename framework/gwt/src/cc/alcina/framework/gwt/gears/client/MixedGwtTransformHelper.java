package cc.alcina.framework.gwt.gears.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
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

	public <T extends DomainModelHolder> T handleLoad(
			LoadObjectsHolder<T> holder, AlcinaRpcRequestBuilder builder) {
		this.holder = holder;
		return holder.getDomainObjects();
	}

	public LoadObjectsRequest prepareRequest(AlcinaRpcRequestBuilder builder) {
		this.builder = builder;
		builder.setRecordResult(true);
		LoadObjectsRequest request = new LoadObjectsRequest();
		request.setClientInstance(ClientLayerLocator.get().getClientInstance());
		return request;
	}

	public void provisionallyReplay() {
		// TODO Auto-generated method stub
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
			ClientUtils.invokeJsDebugger();
			caught.printStackTrace();
		}

		public void onSuccess(LoadObjectsHolder result) {
			holder = result;
		}
	}
}
