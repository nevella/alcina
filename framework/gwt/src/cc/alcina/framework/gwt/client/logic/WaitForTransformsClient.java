package cc.alcina.framework.gwt.client.logic;

import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.HasAnnotationCallback;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.gwt.client.ClientBase;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class WaitForTransformsClient implements AsyncCallback<DomainUpdate> {
	private long lastCommittedRequestId;

	public void start(long maxDbPersistedRequestId) {
		this.lastCommittedRequestId = maxDbPersistedRequestId;
		waitForTransforms();
	}

	private void waitForTransforms() {
		ClientBase.getCommonRemoteServiceAsyncInstance().waitForTransforms(
				lastCommittedRequestId, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		TimerWrapper timer = Registry.impl(TimerWrapperProvider.class)
				.getTimer(() -> {
					waitForTransforms();
				});
		timer.scheduleSingle(5000);
		throw new WrappedRuntimeException(caught);
	}

	@Override
	public void onSuccess(DomainUpdate result) {
		TransformManager tm = TransformManager.get();
		for (DomainTransformRequest dtr : result.requests) {
			long clientInstanceId = dtr.getClientInstance().getId();
			boolean ignoreCreates = ClientBase.getClientInstance().getId() == clientInstanceId;
			try {
				CollectionModificationSupport.queue(true);
				ClientTransformManager.cast()
						.setFirePropertyChangesOnConsumedCollectionMods(true);
				TransformManager.get().setReplayingRemoteEvent(true);
				for (DomainTransformEvent dte : dtr.getEvents()) {
					try {
						// the one thing that would cause schmiel in the graph
						if (dte.getTransformType() == TransformType.CREATE_OBJECT
								&& ignoreCreates) {
							continue;
						}
						tm.consume(dte);
					} catch (DomainTransformException e) {
						if (e.getType() == DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND
								|| e.getType() == DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND) {
						} else {
							throw new WrappedRuntimeException(e);
						}
					}
				}
			} finally {
				TransformManager.get().setReplayingRemoteEvent(false);
				ClientTransformManager.cast()
						.setFirePropertyChangesOnConsumedCollectionMods(false);
				CollectionModificationSupport.queue(false);
			}
		}
		lastCommittedRequestId = result.maxDbPersistedRequestId;
		waitForTransforms();
	}
}
