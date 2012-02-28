package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.GwtRpcProtocolHandler;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientMetricLogging;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.util.ClientUtils;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class SerializedDomainLoader {
	public static final String OFFLINE_LOAD_METRIC_KEY = "offline-load";

	private static final String ONLINE_INITIAL_DESER_METRIC_KEY = "online-initial-deser";

	protected List<DTRSimpleSerialWrapper> transforms;

	protected LoadObjectsHolder<DomainModelHolder> loadObjectsHolder;

	private Iterator<DTRSimpleSerialWrapper> initialEventsTransformIterator;

	private AsyncCallback<List<DomainTransformEvent>> initialEventsIteratorCallback;

	private ArrayList<DomainTransformEvent> initialEvents;

	public abstract void afterEventReplay();

	public abstract ClientInstance beforeEventReplay();

	public abstract DomainModelHolder createDummyModel();

	public void loadSerializedTransformsForOnline(
			final PersistenceCallback<Boolean> persistenceCallback) {
		LocalTransformPersistence localPersistence = LocalTransformPersistence
				.get();
		if (!localPersistence.isLocalStorageInstalled()) {
			persistenceCallback.onSuccess(false);
		}
		ClientMetricLogging.get().start(ONLINE_INITIAL_DESER_METRIC_KEY);
		PersistenceCallback<List<DTRSimpleSerialWrapper>> afterOpenTransforms = new PersistenceCallback<List<DTRSimpleSerialWrapper>>() {
			@Override
			public void onFailure(Throwable caught) {
				persistenceCallback.onSuccess(false);
			}

			@Override
			public void onSuccess(List<DTRSimpleSerialWrapper> result) {
				transforms = result;
				if (!transforms.isEmpty()) {
					final DTRSimpleSerialWrapper wrapper = transforms.get(0);
					DTRProtocolHandler handler = new DTRProtocolSerializer()
							.getHandler(wrapper.getProtocolVersion());
					if (handler instanceof GwtRpcProtocolHandler) {
						AsyncCallback<LoadObjectsHolder> replayRpcCallback = new AsyncCallback<LoadObjectsHolder>() {
							private void _finally() {
								ClientMetricLogging.get().end(
										ONLINE_INITIAL_DESER_METRIC_KEY);
							}

							@Override
							public void onSuccess(LoadObjectsHolder loadObjectsHolder) {
								try {
									ClientNotifications no = ClientLayerLocator
											.get().notifications();
									no.log("replayRpc - exists - "
											+ (loadObjectsHolder != null));
									no.log("replayRpc - domain objects exist - "
											+ (loadObjectsHolder
													.getDomainObjects() != null));
									no.log("replayRpc permutation (ser) - "
											+ (loadObjectsHolder.getRequest()
													.getTypeSignature()));
									no.log("replayRpc permutation(app) - "
											+ getTransformSignature());
									no.log("replayRpc userid (ser)- "
											+ (wrapper.getUserId()));
									no.log("replayRpc userid (app)- "
											+ PermissionsManager.get()
													.getUserId());
									if (loadObjectsHolder != null
											&& loadObjectsHolder
													.getDomainObjects() != null
											&& checkTransformSignature(loadObjectsHolder)
											&& wrapper.getUserId() == PermissionsManager
													.get().getUserId()) {
										setLoadObjectsHolder(loadObjectsHolder);
										persistenceCallback.onSuccess(true);
										return;
									}
								} catch (Exception e) {
									// squelch
								}
								persistenceCallback.onSuccess(false);
							}

							@Override
							public void onFailure(Throwable caught) {
								_finally();
								persistenceCallback.onSuccess(false);
							}
						};
						replayRpc(wrapper.getText(), replayRpcCallback);
						return;
					}
				}
				persistenceCallback.onSuccess(false);
				return;
			}
		};
		localPersistence.openAvailableSessionTransformsForOfflineLoad(false,
				false, afterOpenTransforms);
	}

	protected abstract String getTransformSignature();

	protected abstract boolean checkTransformSignature(
			LoadObjectsHolder loadObjectsHolder);

	public abstract void tryOffline(final Throwable t,
			PersistenceCallback<Boolean> persistenceCallback);

	/*
	 * at most two gwtrpc wrappers - at the top of the list
	 */
	private void handleGwtRpcTransforms(
			AsyncCallback<List<DomainTransformEvent>> callback) {
		initialEvents = new ArrayList<DomainTransformEvent>();
		initialEventsTransformIterator = transforms.iterator();
		initialEventsIteratorCallback = callback;
		iterateInitialEvents();
	}

	private void iterateInitialEvents() {
		for (; initialEventsTransformIterator.hasNext();) {
			DTRSimpleSerialWrapper wrapper = initialEventsTransformIterator
					.next();
			DTRProtocolHandler handler = new DTRProtocolSerializer()
					.getHandler(wrapper.getProtocolVersion());
			if (handler instanceof GwtRpcProtocolHandler) {
				replayRpc(wrapper.getText(),
						new AsyncCallback<LoadObjectsHolder>() {
							@Override
							public void onFailure(Throwable caught) {
								throw new WrappedRuntimeException(caught);
							}

							@Override
							public void onSuccess(LoadObjectsHolder replayRpc) {
								initialEventsTransformIterator.remove();
								if (replayRpc != null) {
									if (replayRpc.getDomainObjects() != null) {
										setLoadObjectsHolder(replayRpc);
									}
									initialEvents.addAll(replayRpc
											.getReplayEvents());
								}
								iterateInitialEvents();
							}
						});
				return;
			}
		}
		initialEventsIteratorCallback.onSuccess(initialEvents);
	}

	private boolean hasGwtRpcTransforms() {
		List<DomainTransformEvent> initialEvents = new ArrayList<DomainTransformEvent>();
		for (Iterator<DTRSimpleSerialWrapper> iterator = transforms.iterator(); iterator
				.hasNext();) {
			DTRSimpleSerialWrapper wrapper = iterator.next();
			DTRProtocolHandler handler = new DTRProtocolSerializer()
					.getHandler(wrapper.getProtocolVersion());
			if (handler instanceof GwtRpcProtocolHandler) {
				return true;
			}
		}
		return false;
	}

	protected void displayReplayRpcNotification(boolean end) {
	}

	protected ClientInstance getDomainObjectsPersistedBy() {
		LocalTransformPersistence localPersistence = LocalTransformPersistence
				.get();
		return localPersistence != null ? localPersistence
				.getDomainObjectsPersistedBy() : null;
	}

	protected LoadObjectsHolder<DomainModelHolder> getLoadObjectsHolder() {
		return loadObjectsHolder;
	}

	protected void registerRpcDomainModelHolder(
			ScheduledCommand scheduledCommand) {
		throw new UnsupportedOperationException();
	}

	protected void replayRpc(String text,
			AsyncCallback<LoadObjectsHolder> callback) {
		((ClientHandshakeHelperWithLocalPersistence) ClientLayerLocator.get()
				.getClientHandshakeHelper()).replayRpc(text, callback);
	}

	protected void setLoadObjectsHolder(
			LoadObjectsHolder<DomainModelHolder> loadObjectsHolder) {
		this.loadObjectsHolder = loadObjectsHolder;
	}

	protected boolean shouldTryOffline(Throwable t,
			LocalTransformPersistence localPersistence) {
		return ClientUtils.maybeOffline(t)
				&& localPersistence.isLocalStorageInstalled();
	}

	protected void tryOfflinePass(Throwable t, boolean notify,
			final PersistenceCallback<Boolean> persistenceCallback) {
		final LocalTransformPersistence localPersistence = LocalTransformPersistence
				.get();
		if (!shouldTryOffline(t, localPersistence)) {
			persistenceCallback.onSuccess(false);
		}
		ClientMetricLogging.get().start(OFFLINE_LOAD_METRIC_KEY);
		PermissionsManager.get().setOnlineState(OnlineState.OFFLINE);
		TransformManager tm = TransformManager.get();
		tm.registerDomainObjectsInHolder(createDummyModel());
		final PersistenceCallback<List<DTRSimpleSerialWrapper>> replayTransformsCallback = new PersistenceCallback<List<DTRSimpleSerialWrapper>>() {
			@Override
			public void onFailure(Throwable caught) {
				ClientMetricLogging.get().end(OFFLINE_LOAD_METRIC_KEY);
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(List<DTRSimpleSerialWrapper> result) {
				transforms = result;
				if (!transforms.isEmpty()
						|| ClientLayerLocator.get().getClientHandshakeHelper()
								.permitsOfflineWithEmptyTransforms()) {
					if (hasGwtRpcTransforms()) {
						displayReplayRpcNotification(false);
					}
					replayAfterPossibleDelay(new ScheduledCommand() {
						@Override
						public void execute() {
							persistenceCallback.onSuccess(true);
						}
					});
				}
			}
		};
		PersistenceCallback<List<DTRSimpleSerialWrapper>> afterOpenForOffline = new PersistenceCallback<List<DTRSimpleSerialWrapper>>() {
			@Override
			public void onFailure(Throwable caught) {
				ClientMetricLogging.get().end(OFFLINE_LOAD_METRIC_KEY);
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(List<DTRSimpleSerialWrapper> result) {
				transforms = result;
				if (transforms.isEmpty()) {
					if (ClientLayerLocator.get().getClientHandshakeHelper()
							.permitsOfflineWithEmptyTransforms()) {
						localPersistence
								.openAvailableSessionTransformsForOfflineLoadNeverOnline(replayTransformsCallback);
					} else {
						persistenceCallback.onSuccess(true);
					}
				} else {
					replayTransformsCallback.onSuccess(result);
				}
			}
		};
		localPersistence.openAvailableSessionTransformsForOfflineLoad(notify,
				afterOpenForOffline);
	}

	protected void replaySequence(final ScheduledCommand postReplayCommand) {
		assert postReplayCommand != null;
		AsyncCallback<List<DomainTransformEvent>> initialEventsCallback = new AsyncCallback<List<DomainTransformEvent>>() {
			@Override
			public void onSuccess(final List<DomainTransformEvent> initialEvents) {
				ScheduledCommand postRegisterCommand = new ScheduledCommand() {
					@Override
					public void execute() {
						replayTransforms(initialEvents);
						postReplayCommand.execute();
					}
				};
				if (getLoadObjectsHolder() != null) {
					registerRpcDomainModelHolder(postRegisterCommand);
				} else {
					postRegisterCommand.execute();
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				throw new WrappedRuntimeException(caught);
			}
		};
		handleGwtRpcTransforms(initialEventsCallback);
	}

	protected void replayAfterPossibleDelay(ScheduledCommand postRegisterCommand) {
		replaySequence(postRegisterCommand);
	}

	protected abstract void replayTransforms(
			List<DomainTransformEvent> initialEvents);
}
