package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientUIThreadWorker;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.GwtRpcProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandler;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.persistence.client.PersistenceCallback.PersistenceCallbackStd;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * <p>
 * <b>Ordering of client transforms</b>
 * </p>
 * <blockquote>
 * <p>
 * Save:
 * </p>
 * <ol>
 * <li>Inital object chunk: async</li>
 * <li>Other (sync from remote, local transforms) - written synchronously</li>
 * </ol>
 * </blockquote> <blockquote>
 * <p>
 * Load (offline):
 * </p>
 * <ol>
 * <li>Initial object chunk (which may be out of order wrt db id)</li>
 * <li>Other (sync from remote, local transforms) - ordered by id</li>
 * </ol>
 * </blockquote>
 * <p>
 * This ensures that offline load is in the correct order
 * 
 * @author nick@alcina.cc
 * 
 */
public abstract class LocalTransformPersistence implements StateChangeListener,
		ClientTransformManager.PersistableTransformListener {
	public static LocalTransformPersistence get() {
		return localTransformPersistence;
	}

	public static void registerLocalTransformPersistence(
			LocalTransformPersistence localTransformPersistence) {
		LocalTransformPersistence.localTransformPersistence = localTransformPersistence;
	}

	private DTESerializationPolicy serializationPolicy;

	private boolean localStorageInstalled = false;

	private CommitToStorageTransformListener commitToStorageTransformListener;

	private Map<Integer, DTRSimpleSerialWrapper> persistedTransforms = new HashMap<Integer, DTRSimpleSerialWrapper>();

	private boolean closing = false;

	private Long clientInstanceIdForGet = null;

	private static LocalTransformPersistence localTransformPersistence;

	private ClientInstance domainObjectsPersistedBy;

	public LocalTransformPersistence() {
	}

	public abstract void clearPersistedClient(ClientInstance exceptFor,
			PersistenceCallback callback);

	public CommitToStorageTransformListener getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
	}

	public Map<Integer, DTRSimpleSerialWrapper> getPersistedTransforms() {
		return this.persistedTransforms;
	}

	public DTESerializationPolicy getSerializationPolicy() {
		return serializationPolicy;
	}

	public void handleUncommittedTransformsOnLoad(final Callback cb) {
		if (!isLocalStorageInstalled()) {
			cb.callback(null);
			return;
		}
		PersistenceCallbackStd<List<DTRSimpleSerialWrapper>> pcb1 = new PersistenceCallbackStd<List<DTRSimpleSerialWrapper>>() {
			@Override
			public void onSuccess(final List<DTRSimpleSerialWrapper> uncommitted) {
				if (!uncommitted.isEmpty()) {
					final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
							"Saving unsaved work from previous session", null);
					crd.getGlass().setOpacity(0);
					AsyncCallback<Void> callback = new AsyncCallback<Void>() {
						public void onFailure(Throwable caught) {
							hideDialog();
							new FromOfflineConflictResolver().resolve(
									uncommitted, caught,
									LocalTransformPersistence.this, cb);
						}

						public void onSuccess(Void result) {
							hideDialog();
							transformPersisted(uncommitted,
									new PersistenceCallbackStd() {
										@Override
										public void onSuccess(Object result) {
											ClientLayerLocator
													.get()
													.notifications()
													.notifyOfCompletedSaveFromOffline();
											cb.callback(null);
										}
									});
						}

						private void hideDialog() {
							crd.hide();
						}
					};
					crd.show();
					ClientLayerLocator.get().commonRemoteServiceAsyncInstance()
							.persistOfflineTransforms(uncommitted, callback);
					return;
				} else {
					cb.callback(null);
				}
			}
		};
		getTransforms(DomainTransformRequestType.TO_REMOTE, pcb1);
	}

	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			PersistenceCallback<Void> callback) {
		setSerializationPolicy(dteSerializationPolicy);
		setCommitToStorageTransformListener(commitToServerTransformListener);
		callback.onSuccess(null);
	}

	public boolean isClosing() {
		return closing;
	}

	public boolean isLocalStorageInstalled() {
		return this.localStorageInstalled;
	}

	public void openAvailableSessionTransformsForOfflineLoad(
			boolean finalPass,
			PersistenceCallback<List<DTRSimpleSerialWrapper>> persistenceCallback) {
		openAvailableSessionTransformsForOfflineLoad(finalPass, true,
				persistenceCallback);
	}

	public void openAvailableSessionTransformsForOfflineLoad(
			final boolean finalPass,
			boolean checkSoleOpenTab,
			final PersistenceCallback<List<DTRSimpleSerialWrapper>> persistenceCallback) {
		try {
			final List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
			if (checkSoleOpenTab && !ClientSession.get().isSoleOpenTab()) {
				if (finalPass) {
					showOfflineLimitMessage();
				}
				persistenceCallback.onSuccess(transforms);
				return;
			}
			final PersistenceCallback<List<DTRSimpleSerialWrapper>> pcb2 = new PersistenceCallback<List<DTRSimpleSerialWrapper>>() {
				@Override
				public void onSuccess(List<DTRSimpleSerialWrapper> result) {
					transforms.addAll(result);
					setClientInstanceIdForGet(null);
					persistenceCallback.onSuccess(transforms);
					return;
				}

				@Override
				public void onFailure(Throwable caught) {
					persistenceCallback.onFailure(caught);
				}
			};
			PersistenceCallback<List<DTRSimpleSerialWrapper>> pcb1 = new PersistenceCallback<List<DTRSimpleSerialWrapper>>() {
				@Override
				public void onSuccess(List<DTRSimpleSerialWrapper> loads) {
					if (loads.size() == 0) {
						// should never happen (or very rarely)
						if (finalPass) {
							showUnableToLoadOfflineMessage();
						}
						persistenceCallback.onSuccess(transforms);
						return;
					}
					DTRSimpleSerialWrapper loadWrapper = loads.iterator()
							.next();
					long clientInstanceId = loadWrapper.getClientInstanceId();
					domainObjectsPersistedBy = ((ClientHandshakeHelperWithLocalPersistence) ClientLayerLocator
							.get().getClientHandshakeHelper())
							.createClientInstance(clientInstanceId,
									loadWrapper.getClientInstanceAuth());
					for (DTRSimpleSerialWrapper wrapper : loads) {
						if (wrapper.getClientInstanceId() != clientInstanceId) {
							persistenceCallback
									.onFailure(new WrappedRuntimeException(
											"Multiple client object loads",
											SuggestedAction.NOTIFY_WARNING));
						}
					}
					setClientInstanceIdForGet(clientInstanceId);
					transforms.add(loadWrapper);
					getTransforms(new DomainTransformRequestType[] {
							DomainTransformRequestType.TO_REMOTE_COMPLETED,
							DomainTransformRequestType.TO_REMOTE,
							DomainTransformRequestType.CLIENT_SYNC }, pcb2);
				}

				@Override
				public void onFailure(Throwable caught) {
					persistenceCallback.onFailure(caught);
				}
			};
			getTransforms(DomainTransformRequestType.CLIENT_OBJECT_LOAD, pcb1);
		} catch (Exception e) {
			persistenceCallback.onFailure(e);
		}
	}

	public void openAvailableSessionTransformsForOfflineLoadNeverOnline(
			PersistenceCallback<List<DTRSimpleSerialWrapper>> persistenceCallback) {
		getTransforms(new DomainTransformRequestType[] {
				DomainTransformRequestType.TO_REMOTE_COMPLETED,
				DomainTransformRequestType.TO_REMOTE,
				DomainTransformRequestType.CLIENT_SYNC }, persistenceCallback);
	}

	public void persistableTransform(DomainTransformRequest dtr) {
		if (dtr.getDomainTransformRequestType() == DomainTransformRequestType.CLIENT_OBJECT_LOAD) {
			dtr.setProtocolVersion(getSerializationPolicy()
					.getInitialObjectPersistenceProtocol());
		} else {
			dtr.setProtocolVersion(getSerializationPolicy()
					.getTransformPersistenceProtocol());
		}
		if (!dtr.getEvents().isEmpty()) {
			if (!closing) {
				new DTRAsyncSerializer(dtr).start();
			} else {
				DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
						dtr, false);
				persist(wrapper, PersistenceCallback.VOID_CALLBACK);
			}
		}
	}

	public void persistInitialRpcPayload(MixedGwtTransformHelper mixedHelper,
			PersistenceCallback<Void> persistenceCallback) {
		// TODO - if transforms, delete all but first clientObjectLoad (and
		// reparent) - if
		// not, delete all
		ClientInstance clientInstance = ClientLayerLocator.get()
				.getClientInstance();
		String rpcResult = mixedHelper.getBuilder().getRpcResult();
		if (rpcResult == null) {
			persistenceCallback.onSuccess(null);
			return;
		}
		DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(0,
				rpcResult, System.currentTimeMillis(), PermissionsManager.get()
						.getUserId(), clientInstance.getId(), 0,
				clientInstance.getAuth(),
				DomainTransformRequestType.CLIENT_OBJECT_LOAD,
				GwtRpcProtocolHandler.VERSION, "");
		persist(wrapper, persistenceCallback);
	}

	public void setClosing(boolean closing) {
		this.closing = closing;
	}

	public void setCommitToStorageTransformListener(
			CommitToStorageTransformListener commitToStorageTransformListener) {
		this.commitToStorageTransformListener = commitToStorageTransformListener;
	}

	public void setSerializationPolicy(
			DTESerializationPolicy serializationPolicy) {
		this.serializationPolicy = serializationPolicy;
	}

	public boolean shouldPersistClient(boolean clientSupportsRpcPersistence) {
		return !ClientSession.get().isInitialObjectsPersisted()
				|| clientSupportsRpcPersistence;
	}

	public void stateChanged(Object source, String newState) {
		if (newState == CommitToStorageTransformListener.COMMITTING) {
			List<DomainTransformRequest> rqs = getCommitToStorageTransformListener()
					.getPriorRequestsWithoutResponse();
			for (DomainTransformRequest rq : rqs) {
				final int requestId = rq.getRequestId();
				if (!getPersistedTransforms().containsKey(requestId)
						&& !rq.getEvents().isEmpty()) {
					rq.setProtocolVersion(getSerializationPolicy()
							.getTransformPersistenceProtocol());
					final DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
							rq);
					persist(wrapper, new PersistenceCallbackStd<Void>() {
						@Override
						public void onSuccess(Void result) {
							getPersistedTransforms().put(requestId, wrapper);
						}
					});
				}
			}
		} else if (newState == CommitToStorageTransformListener.COMMITTED) {
			List<DomainTransformRequest> rqs = getCommitToStorageTransformListener()
					.getPriorRequestsWithoutResponse();
			final Set<Integer> removeIds = new HashSet(getPersistedTransforms()
					.keySet());
			for (DomainTransformRequest rq : rqs) {
				removeIds.remove(rq.getRequestId());
			}
			List<DTRSimpleSerialWrapper> persistedWrappers = new ArrayList<DTRSimpleSerialWrapper>();
			for (Integer i : removeIds) {
				DTRSimpleSerialWrapper wrapper = getPersistedTransforms()
						.get(i);
				persistedWrappers.add(wrapper);
			}
			PersistenceCallback<Void> afterTransformsMarkedAsPersistedCallback = new PersistenceCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onSuccess(Void result) {
					for (Integer i : removeIds) {
						getPersistedTransforms().remove(i);
					}
					DomainTransformRequest rq = new DomainTransformRequest();
					rq.setClientInstance(ClientLayerLocator.get()
							.getClientInstance());
					rq.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_SYNC);
					rq.setRequestId(0);
					rq.setEvents(new ArrayList<DomainTransformEvent>(
							getCommitToStorageTransformListener()
									.getSynthesisedEvents()));
					rq.setProtocolVersion(getSerializationPolicy()
							.getTransformPersistenceProtocol());
					DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
							rq);
					persist(wrapper, PersistenceCallback.VOID_CALLBACK);
				}
			};
			transformPersisted(persistedWrappers,
					afterTransformsMarkedAsPersistedCallback);
			return;
		} else if (newState == CommitToStorageTransformListener.RELOAD) {
			clearAllPersisted(PersistenceCallback.VOID_CALLBACK);
		}
	}

	protected abstract void clearAllPersisted(PersistenceCallback callback);

	protected Long getClientInstanceIdForGet() {
		return clientInstanceIdForGet;
	}

	protected void getTransforms(DomainTransformRequestType type,
			PersistenceCallback<List<DTRSimpleSerialWrapper>> callback) {
		getTransforms(new DomainTransformRequestType[] { type }, callback);
	}

	protected abstract void getTransforms(DomainTransformRequestType[] types,
			PersistenceCallback<List<DTRSimpleSerialWrapper>> callback);

	protected abstract void persist(DTRSimpleSerialWrapper wrapper,
			PersistenceCallback callback);

	protected void persistAndReparentClientLoadTransforms(
			final MixedGwtTransformHelper mixedHelper,
			final PersistenceCallback<Void> persistenceCallback) {
		final PersistenceCallback afterReparentTransforms = new PersistenceCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				MixedGwtLoadException lex = null;
				lex = (MixedGwtLoadException) ((caught instanceof MixedGwtLoadException) ? caught
						: new MixedGwtLoadException(caught));
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Void result) {
				persistInitialRpcPayload(mixedHelper, persistenceCallback);
			}
		};
		PersistenceCallback afterGetTransforms = new PersistenceCallback<List<DTRSimpleSerialWrapper>>() {
			@Override
			public void onFailure(Throwable caught) {
				MixedGwtLoadException lex = null;
				lex = (MixedGwtLoadException) ((caught instanceof MixedGwtLoadException) ? caught
						: new MixedGwtLoadException(caught));
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(List<DTRSimpleSerialWrapper> loads) {
				DTRSimpleSerialWrapper rpcWrapper;
				try {
					if (loads.size() == 0) {
						throw new MixedGwtLoadException(
								"Hmm...our load disappeared. Dang. ", false);
					}
					rpcWrapper = loads.get(0);
					if (rpcWrapper.getUserId() != PermissionsManager.get()
							.getUserId()) {
						throw new MixedGwtLoadException(
								"Hmm...our load was hijacked by another user. Dang. ",
								false);
					}
				} catch (MixedGwtLoadException e) {
					onFailure(e);
					return;
				}
				reparentToClientInstance(rpcWrapper, ClientLayerLocator.get()
						.getClientInstance(), afterReparentTransforms);
			}
		};
		getTransforms(DomainTransformRequestType.CLIENT_OBJECT_LOAD,
				afterGetTransforms);
	}

	protected abstract void reparentToClientInstance(
			DTRSimpleSerialWrapper wrapper, ClientInstance clientInstance,
			PersistenceCallback callback);

	protected void setClientInstanceIdForGet(Long clientInstanceIdForGet) {
		this.clientInstanceIdForGet = clientInstanceIdForGet;
	}

	protected void setLocalStorageInstalled(boolean localStorageInstalled) {
		this.localStorageInstalled = localStorageInstalled;
	}

	protected void setPersistedTransforms(
			Map<Integer, DTRSimpleSerialWrapper> persistedTransforms) {
		this.persistedTransforms = persistedTransforms;
	}

	protected void showOfflineLimitMessage() {
		ClientLayerLocator
				.get()
				.notifications()
				.showError(
						"Unable to open offline session",
						new Exception("Only one tab may be open "
								+ "for this application when opening offline. "));
	}

	protected void showUnableToLoadOfflineMessage() {
		ClientLayerLocator
				.get()
				.notifications()
				.showMessage(
						"<b>Unable to open offline session</b><br><br>"
								+ "No data saved");
	}

	protected abstract void transformPersisted(
			List<DTRSimpleSerialWrapper> persistedWrappers,
			PersistenceCallback callback);

	ClientInstance getDomainObjectsPersistedBy() {
		return this.domainObjectsPersistedBy;
	}

	protected class DTRAsyncSerializer extends ClientUIThreadWorker {
		DTRSimpleSerialWrapper wrapper;

		StringBuffer sb = new StringBuffer();

		private List<DomainTransformEvent> items;

		public DTRAsyncSerializer(DomainTransformRequest dtr) {
			super(1000, 200);
			wrapper = new DTRSimpleSerialWrapper(dtr, true);
			items = dtr.getEvents();
		}

		@Override
		protected boolean isComplete() {
			return index == items.size();
		}

		protected void onComplete() {
			ClientLayerLocator.get().notifications().metricLogStart("persist");
			wrapper.setText(sb.toString());
			persist(wrapper, new PersistenceCallbackStd() {
				@Override
				public void onSuccess(Object result) {
					ClientLayerLocator.get().notifications()
							.metricLogEnd("persist");
				}
			});
		}

		@Override
		protected void performIteration() {
			int max = Math.min(index + iterationCount, items.size());
			StringBuffer sb2 = new StringBuffer();
			lastPassIterationsPerformed = max - index;
			DTRProtocolHandler handler = new DTRProtocolSerializer()
					.getHandler(PlaintextProtocolHandler.VERSION);
			for (; index < max; index++) {
				handler.appendTo(items.get(index), sb2);
			}
			sb.append(sb2.toString());
		}
	}

	public abstract String getPersistenceStoreName();
}
