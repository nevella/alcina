package cc.alcina.framework.gwt.client.logic;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.gwt.client.ClientBase;

@RegistryLocation(registryPoint = WaitForTransformsClient.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class WaitForTransformsClient implements AsyncCallback<DomainUpdate> {
	public static WaitForTransformsClient get() {
		return Registry.impl(WaitForTransformsClient.class);
	}

	private DomainTransformCommitPosition position;

	public DomainTransformCommitPosition getPosition() {
		return this.position;
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
			ClientInstance clientInstance = PermissionsManager.get()
					.getClientInstance();
			boolean ignoreCreates = clientInstance.getId() == clientInstanceId;
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
						tm.apply(dte);
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
		position = result.commitPosition;
		waitForTransforms();
	}

	public void start(DomainTransformCommitPosition initialPosition) {
		this.position = initialPosition;
		waitForTransforms();
	}

	private void waitForTransforms() {
		ClientBase.getCommonRemoteServiceAsyncInstance()
				.waitForTransforms(position, this);
	}
}
