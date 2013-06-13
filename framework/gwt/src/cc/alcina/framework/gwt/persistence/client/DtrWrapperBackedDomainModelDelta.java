package cc.alcina.framework.gwt.persistence.client;

import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.RepeatingCommandWithPostCompletionCallback;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.GwtRpcProtocolHandler;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.Converter;

public class DtrWrapperBackedDomainModelDelta implements DomainModelDelta {
	private DTRSimpleSerialWrapper wrapper;

	LoadObjectsHolder holder;

	List<DomainTransformEvent> transforms;

	boolean unwrapped;

	DomainTransformRequest uncomittedDomainTransformRequest = null;

	public DtrWrapperBackedDomainModelDelta(DTRSimpleSerialWrapper wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	public DomainModelHolder getDomainModelHolder() {
		return holder == null ? null : holder.getDomainModelHolder();
	}

	@Override
	public Collection<HasIdAndLocalId> getUnlinkedObjects() {
		return holder == null ? null : holder.getUnlinkedObjects();
	}

	@Override
	public Collection<DomainTransformEvent> getReplayEvents() {
		return transforms != null ? transforms : holder != null ? holder
				.getReplayEvents() : null;
	}

	@Override
	public String getAppInstruction() {
		return holder == null ? null : holder.getAppInstruction();
	}

	static class DtrWrapperToDomainModelDeltaConverter implements
			Converter<DTRSimpleSerialWrapper, DomainModelDelta> {
		@Override
		public DomainModelDelta convert(DTRSimpleSerialWrapper wrapper) {
			return new DtrWrapperBackedDomainModelDelta(wrapper);
		}
	}

	@Override
	public DomainTransformRequest getUncomittedDomainTransformRequest() {
		if (uncomittedDomainTransformRequest == null
				&& wrapper.getDomainTransformRequestType() == DomainTransformRequestType.TO_REMOTE) {
			uncomittedDomainTransformRequest = new DomainTransformRequest();
			uncomittedDomainTransformRequest.setRequestId(wrapper
					.getRequestId());
			uncomittedDomainTransformRequest.setClientInstance(null);
			uncomittedDomainTransformRequest
					.setDomainTransformRequestType(DomainTransformRequestType.TO_REMOTE);
			uncomittedDomainTransformRequest.setTag(wrapper.getTag());
		}
		return uncomittedDomainTransformRequest;
	}

	@Override
	public Integer getDomainTransformRequestReplayId() {
		if (wrapper != null) {
			DomainTransformRequestType type = wrapper
					.getDomainTransformRequestType();
			if (type == DomainTransformRequestType.TO_REMOTE
					|| type == DomainTransformRequestType.TO_REMOTE_COMPLETED) {
				return wrapper.getRequestId();
			}
		}
		return null;
	}

	@Override
	public void unwrap(AsyncCallback<Void> completionCallback) {
		if (unwrapped) {
			completionCallback.onSuccess(null);
			return;
		}
		unwrapped = true;
		if (wrapper.getProtocolVersion().equals(GwtRpcProtocolHandler.VERSION)) {
			LoadObjectsRequest request = new LoadObjectsRequest();
			Registry.impl(RpcDeserialiser.class).deserialize(
					LoadObjectsHolder.class, wrapper.getText(),
					new GwtRpcDeserialisationCallback(completionCallback));
		} else {
			DTEAsyncDeserializer deserializer = new DTEAsyncDeserializer(
					wrapper);
			transforms = deserializer.getItems();
			RepeatingCommandWithPostCompletionCallback deserializeRunner = new RepeatingCommandWithPostCompletionCallback(
					completionCallback, deserializer);
			// fw_java_3 - generalise
			Scheduler.get().scheduleIncremental(deserializeRunner);
		}
	}

	@Override
	public String getTypeSignature() {
		return holder == null ? null : holder.getRequest().getTypeSignature();
	}

	public Long getLastTransformId() {
		return holder == null ? null : holder.getLastTransformId();
	}

	class GwtRpcDeserialisationCallback implements
			AsyncCallback<LoadObjectsHolder> {
		private AsyncCallback<Void> completionCallback;

		public GwtRpcDeserialisationCallback(
				AsyncCallback<Void> completionCallback) {
			this.completionCallback = completionCallback;
		}

		@Override
		public void onFailure(Throwable caught) {
			completionCallback.onFailure(caught);
		}

		@Override
		public void onSuccess(LoadObjectsHolder result) {
			holder = result;
			completionCallback.onSuccess(null);
		}
	}

	public DTRSimpleSerialWrapper getWrapper() {
		return this.wrapper;
	}
}