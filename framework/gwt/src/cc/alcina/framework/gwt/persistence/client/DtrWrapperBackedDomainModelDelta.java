package cc.alcina.framework.gwt.persistence.client;

import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.RepeatingCommandWithPostCompletionCallback;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaEntity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaMetadata;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaSignature;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObject;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.HasRequestReplayId;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DomainTrancheProtocolHandler;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;

public class DtrWrapperBackedDomainModelDelta
		implements DomainModelDelta, HasRequestReplayId {
	private DeltaApplicationRecord wrapper;

	DomainModelDelta referencedDelta;

	List<DomainTransformEvent> transforms;

	boolean unwrapped;

	DomainTransformRequest uncomittedDomainTransformRequest = null;

	public DtrWrapperBackedDomainModelDelta(DeltaApplicationRecord wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	public String getAppInstruction() {
		return referencedDelta == null ? null
				: referencedDelta.getAppInstruction();
	}

	@Override
	public DomainModelHolder getDomainModelHolder() {
		return referencedDelta == null ? null
				: referencedDelta.getDomainModelHolder();
	}

	@Override
	public DomainModelObject getDomainModelObject() {
		return referencedDelta == null ? null
				: referencedDelta.getDomainModelObject();
	}

	@Override
	public Integer getDomainTransformRequestReplayId() {
		if (wrapper != null) {
			DeltaApplicationRecordType type = wrapper.getType();
			if (type == DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED
					|| type == DeltaApplicationRecordType.LOCAL_TRANSFORMS_REMOTE_PERSISTED) {
				return wrapper.getRequestId();
			}
		}
		return null;
	}

	@Override
	public DomainModelDeltaMetadata getMetadata() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<DomainTransformEvent> getReplayEvents() {
		return transforms != null ? transforms
				: referencedDelta != null ? referencedDelta.getReplayEvents()
						: null;
	}

	@Override
	public DomainModelDeltaSignature getSignature() {
		return DomainModelDeltaSignature.parseSignature(wrapper.getText());
	}

	@Override
	public Collection<DomainModelDeltaEntity> getUnlinkedObjects() {
		return referencedDelta == null ? null
				: referencedDelta.getUnlinkedObjects();
	}

	public DeltaApplicationRecord getWrapper() {
		return this.wrapper;
	}

	@Override
	public boolean hasLocalOnlyTransforms() {
		return wrapper
				.getType() == DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED;
	}

	@Override
	public void unwrap(final AsyncCallback<Void> completionCallback) {
		if (unwrapped) {
			completionCallback.onSuccess(null);
			return;
		}
		unwrapped = true;
		if (wrapper.getProtocolVersion()
				.equals(DomainTrancheProtocolHandler.VERSION)) {
			AsyncCallback<DomainModelDelta> trancheCallback = new AsyncCallbackStd<DomainModelDelta>() {
				@Override
				public void onSuccess(DomainModelDelta result) {
					referencedDelta = result;
					completionCallback.onSuccess(null);
				}
			};
			DeltaStore.get().getDelta(getSignature(), trancheCallback);
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

	public static class DeltaApplicationRecordToDomainModelDeltaConverter
			implements Converter<DeltaApplicationRecord, DomainModelDelta> {
		@Override
		public DomainModelDelta convert(DeltaApplicationRecord wrapper) {
			return new DtrWrapperBackedDomainModelDelta(wrapper);
		}
	}
}