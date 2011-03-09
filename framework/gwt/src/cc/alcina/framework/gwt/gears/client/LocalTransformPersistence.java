package cc.alcina.framework.gwt.gears.client;

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

import com.google.gwt.gears.client.GearsException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
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
	private DTESerializationPolicy serializationPolicy;

	private boolean localStorageInstalled = false;

	private CommitToStorageTransformListener commitToStorageTransformListener;

	private Map<Integer, DTRSimpleSerialWrapper> persistedTransforms = new HashMap<Integer, DTRSimpleSerialWrapper>();

	private boolean closing = false;

	private Long clientInstanceIdForGet = null;

	private static LocalTransformPersistence localTransformPersistence;

	public void setSerializationPolicy(
			DTESerializationPolicy serializationPolicy) {
		this.serializationPolicy = serializationPolicy;
	}

	public boolean isLocalStorageInstalled() {
		return this.localStorageInstalled;
	}

	public DTESerializationPolicy getSerializationPolicy() {
		return serializationPolicy;
	}

	public abstract void clearPersistedClient(ClientInstance exceptFor);

	public void setCommitToStorageTransformListener(
			CommitToStorageTransformListener commitToStorageTransformListener) {
		this.commitToStorageTransformListener = commitToStorageTransformListener;
	}

	public void stateChanged(Object source, String newState) {
		if (newState == CommitToStorageTransformListener.COMMITTING) {
			List<DomainTransformRequest> rqs = getCommitToStorageTransformListener()
					.getPriorRequestsWithoutResponse();
			for (DomainTransformRequest rq : rqs) {
				int requestId = rq.getRequestId();
				if (!getPersistedTransforms().containsKey(requestId)
						&& !rq.getEvents().isEmpty()) {
					rq.setProtocolVersion(getSerializationPolicy()
							.getTransformPersistenceProtocol());
					DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
							rq);
					persist(wrapper);
					getPersistedTransforms().put(requestId, wrapper);
				}
			}
		} else if (newState == CommitToStorageTransformListener.COMMITTED) {
			List<DomainTransformRequest> rqs = getCommitToStorageTransformListener()
					.getPriorRequestsWithoutResponse();
			Set<Integer> removeIds = new HashSet(getPersistedTransforms()
					.keySet());
			for (DomainTransformRequest rq : rqs) {
				removeIds.remove(rq.getRequestId());
			}
			for (Integer i : removeIds) {
				DTRSimpleSerialWrapper wrapper = getPersistedTransforms()
						.get(i);
				transformPersisted(wrapper);
				getPersistedTransforms().remove(i);
			}
			DomainTransformRequest rq = new DomainTransformRequest();
			rq.setClientInstance(ClientLayerLocator.get().getClientInstance());
			rq.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_SYNC);
			rq.setRequestId(0);
			rq.setEvents(new ArrayList<DomainTransformEvent>(
					getCommitToStorageTransformListener()
							.getSynthesisedEvents()));
			rq.setProtocolVersion(getSerializationPolicy()
					.getTransformPersistenceProtocol());
			DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(rq);
			persist(wrapper);
		} else if (newState == CommitToStorageTransformListener.RELOAD) {
			clearAllPersisted();
		}
	}

	public void persistInitialRpcPayload(MixedGwtTransformHelper mixedHelper) {
		// TODO - if transforms, delete all but first c_o_l (and reparent) - if
		// not, delete all
		ClientInstance clientInstance = ClientLayerLocator.get()
				.getClientInstance();
		DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(0,
				mixedHelper.getBuilder().getRpcResult(),
				System.currentTimeMillis(), PermissionsManager.get()
						.getUserId(), clientInstance.getId(), 0,
				clientInstance.getAuth(),
				DomainTransformRequestType.CLIENT_OBJECT_LOAD,
				GwtRpcProtocolHandler.VERSION, "");
		persist(wrapper);
	}

	protected abstract void clearAllPersisted();

	protected abstract void transformPersisted(DTRSimpleSerialWrapper wrapper);

	protected abstract void persist(DTRSimpleSerialWrapper wrapper);

	public Map<Integer, DTRSimpleSerialWrapper> getPersistedTransforms() {
		return this.persistedTransforms;
	}

	public CommitToStorageTransformListener getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
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
				persist(wrapper);
			}
		}
	}

	public void handleUncommittedTransformsOnLoad(final Callback cb) {
		if (!isLocalStorageInstalled()) {
			cb.callback(null);
			return;
		}
		try {
			final List<DTRSimpleSerialWrapper> uncommitted = getTransforms(DomainTransformRequestType.TO_REMOTE);
			if (!uncommitted.isEmpty()) {
				final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
						"Saving unsaved work from previous session", null);
				crd.getGlass().setOpacity(0);
				AsyncCallback<Void> callback = new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						hideDialog();
						new FromOfflineConflictResolver().resolve(uncommitted,
								caught, LocalTransformPersistence.this, cb);
					}

					public void onSuccess(Void result) {
						hideDialog();
						clearAllPersisted();
						Window.alert("Save work from previous session to server completed");
						cb.callback(null);
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
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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
			persist(wrapper);
			ClientLayerLocator.get().notifications().metricLogEnd("persist");
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

	protected abstract List<DTRSimpleSerialWrapper> getTransforms(
			DomainTransformRequestType[] types) throws Exception;

	public boolean isClosing() {
		return closing;
	}

	public void setClosing(boolean closing) {
		this.closing = closing;
	}

	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener) {
		setSerializationPolicy(dteSerializationPolicy);
		setCommitToStorageTransformListener(commitToServerTransformListener);
	}

	protected List<DTRSimpleSerialWrapper> getTransforms(
			DomainTransformRequestType type) throws Exception {
		return getTransforms(new DomainTransformRequestType[] { type });
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

	public boolean shouldPersistClient(boolean clientSupportsRpcPersistence)
			throws GearsException {
		return !ClientSession.get().isInitialObjectsPersisted()
				|| clientSupportsRpcPersistence;
	}

	private ClientInstance domainObjectsPersistedBy;

	ClientInstance getDomainObjectsPersistedBy() {
		return this.domainObjectsPersistedBy;
	}

	protected void persistAndReparentClientLoadTransforms(
			MixedGwtTransformHelper mixedHelper) throws MixedGwtLoadException {
		try {
			List<DTRSimpleSerialWrapper> loads = getTransforms(DomainTransformRequestType.CLIENT_OBJECT_LOAD);
			if (loads.size() == 0) {
				throw new MixedGwtLoadException(
						"Hmm...our load disappeared. Dang. ", false);
			}
			DTRSimpleSerialWrapper rpcWrapper = loads.get(0);
			if (rpcWrapper.getUserId() != PermissionsManager.get().getUserId()) {
				throw new MixedGwtLoadException(
						"Hmm...our load was hijacked by another user. Dang. ",
						false);
			}
			reparentToClientInstance(rpcWrapper, ClientLayerLocator.get()
					.getClientInstance());
			persistInitialRpcPayload(mixedHelper);
		} catch (Exception e) {
			MixedGwtLoadException lex = null;
			lex = (MixedGwtLoadException) ((e instanceof MixedGwtLoadException) ? e
					: new MixedGwtLoadException(e));
			throw lex;
		}
	}

	protected abstract void reparentToClientInstance(
			DTRSimpleSerialWrapper wrapper, ClientInstance clientInstance);

	public List<DTRSimpleSerialWrapper> openAvailableSessionTransformsForOfflineLoad(
			boolean finalPass) {
		return openAvailableSessionTransformsForOfflineLoad(finalPass, true);
	}

	public List<DTRSimpleSerialWrapper> openAvailableSessionTransformsForOfflineLoadNeverOnline() {
		try {
			List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
			transforms.addAll(getTransforms(new DomainTransformRequestType[] {
					DomainTransformRequestType.TO_REMOTE_COMPLETED,
					DomainTransformRequestType.TO_REMOTE,
					DomainTransformRequestType.CLIENT_SYNC }));
			return transforms;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public List<DTRSimpleSerialWrapper> openAvailableSessionTransformsForOfflineLoad(
			boolean finalPass, boolean checkSoleOpenTab) {
		try {
			List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
			if (checkSoleOpenTab && !ClientSession.get().isSoleOpenTab()) {
				if (finalPass) {
					showOfflineLimitMessage();
				}
				return transforms;
			}
			List<DTRSimpleSerialWrapper> loads = getTransforms(DomainTransformRequestType.CLIENT_OBJECT_LOAD);
			if (loads.size() == 0) {
				// should never happen (or very rarely)
				if (finalPass) {
					showUnableToLoadOfflineMessage();
				}
				return transforms;
			}
			DTRSimpleSerialWrapper loadWrapper = loads.iterator().next();
			long clientInstanceId = loadWrapper.getClientInstanceId();
			domainObjectsPersistedBy = ((ClientHandshakeHelperWithLocalPersistence) ClientLayerLocator
					.get().getClientHandshakeHelper()).createClientInstance(
					clientInstanceId, loadWrapper.getClientInstanceAuth());
			for (DTRSimpleSerialWrapper wrapper : loads) {
				if (wrapper.getClientInstanceId() != clientInstanceId) {
					throw new WrappedRuntimeException(
							"Multiple client object loads",
							SuggestedAction.NOTIFY_WARNING);
				}
			}
			setClientInstanceIdForGet(clientInstanceId);
			transforms.add(loadWrapper);
			transforms.addAll(getTransforms(new DomainTransformRequestType[] {
					DomainTransformRequestType.TO_REMOTE_COMPLETED,
					DomainTransformRequestType.TO_REMOTE,
					DomainTransformRequestType.CLIENT_SYNC }));
			setClientInstanceIdForGet(null);
			return transforms;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void setClientInstanceIdForGet(Long clientInstanceIdForGet) {
		this.clientInstanceIdForGet = clientInstanceIdForGet;
	}

	protected Long getClientInstanceIdForGet() {
		return clientInstanceIdForGet;
	}

	public static void registerLocalTransformPersistence(
			LocalTransformPersistence localTransformPersistence) {
		LocalTransformPersistence.localTransformPersistence = localTransformPersistence;
	}

	public static LocalTransformPersistence get() {
		return localTransformPersistence;
	}

	public LocalTransformPersistence() {
	}
}
