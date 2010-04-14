package cc.alcina.framework.gwt.gears.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.gears.client.GearsException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.ClientWorker;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.PersistableTransformListener;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

public abstract class AbstractTransformPersistence implements
		StateChangeListener, PersistableTransformListener {
	private DTESerializationPolicy serializationPolicy;

	private boolean localStorageInstalled = false;

	private CommitToStorageTransformListener commitToStorageTransformListener;

	private Map<Integer, DTRSimpleSerialWrapper> persistedTransforms = new HashMap<Integer, DTRSimpleSerialWrapper>();

	private boolean closing = false;

	private Long clientInstanceIdForGet = null;

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
						&& !rq.getItems().isEmpty()) {
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
			rq.setClientInstance(getCommitToStorageTransformListener()
					.getClientInstance());
			rq
					.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_SYNC);
			rq.setRequestId(0);
			rq.setItems(new ArrayList<DomainTransformEvent>(
					getCommitToStorageTransformListener()
							.getSynthesisedEvents()));
			DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(rq);
			persist(wrapper);
		}
	}

	protected abstract void clearPersisted();

	protected abstract void transformPersisted(DTRSimpleSerialWrapper wrapper);

	protected abstract void persist(DTRSimpleSerialWrapper wrapper);

	public Map<Integer, DTRSimpleSerialWrapper> getPersistedTransforms() {
		return this.persistedTransforms;
	}

	public CommitToStorageTransformListener getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
	}

	public void persistableTransform(DomainTransformRequest dtr) {
		if (!dtr.getItems().isEmpty()) {
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
						new SimpleConflictResolver().resolve(uncommitted,
								caught, AbstractTransformPersistence.this, cb);
					}

					public void onSuccess(Void result) {
						hideDialog();
						clearPersisted();
						Window
								.alert("Save work from previous session to server completed");
						cb.callback(null);
					}

					private void hideDialog() {
						crd.hide();
					}
				};
				crd.show();
				ClientLayerLocator.get().commonRemoteServiceAsync()
						.persistOfflineTransforms(uncommitted, callback);
				return;
			} else {
				cb.callback(null);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected class DTRAsyncSerializer extends ClientWorker {
		DTRSimpleSerialWrapper wrapper;

		StringBuffer sb = new StringBuffer();

		private List<DomainTransformEvent> items;

		public DTRAsyncSerializer(DomainTransformRequest dtr) {
			super(1000, 200);
			wrapper = new DTRSimpleSerialWrapper(dtr, true);
			items = dtr.getItems();
		}

		@Override
		protected boolean isComplete() {
			return index == items.size();
		}

		protected void onComplete() {
			ClientLayerLocator.get().clientBase().metricLogStart("persist");
			wrapper.setText(sb.toString());
			persist(wrapper);
			ClientLayerLocator.get().clientBase().metricLogEnd("persist");
		}

		@Override
		protected void performIteration() {
			int max = Math.min(index + iterationCount, items.size());
			StringBuffer sb2 = new StringBuffer();
			lastPassIterationsPerformed = max - index;
			for (; index < max; index++) {
				items.get(index).appendTo(sb2);
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

	protected List<DTRSimpleSerialWrapper> getTransforms(DomainTransformRequestType type) throws Exception {
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
		ClientLayerLocator.get().clientBase().showError(
				"Unable to open offline session",
				new Exception("Only one tab may be open "
						+ "for this application when opening offline. "));
	}

	protected void showUnableToLoadOfflineMessage() {
		ClientLayerLocator.get().clientBase().showMessage(
				"<b>Unable to open offline session</b><br><br>"
						+ "No data saved");
	}

	public boolean shouldPersistClient() throws GearsException {
		return !ClientSession.get().isInitialObjectsPersisted();
	}

	public List<DTRSimpleSerialWrapper> openAvailableSessionTransformsForOfflineLoad() {
		try {
			List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
			if (!ClientSession.get().isSoleOpenTab()) {
				showOfflineLimitMessage();
				return transforms;
			}
			List<DTRSimpleSerialWrapper> loads = getTransforms(DomainTransformRequestType.CLIENT_OBJECT_LOAD);
			if (loads.size() == 0) {
				// should never happen (or very rarely)
				showUnableToLoadOfflineMessage();
				return transforms;
			}
			if (loads.size() != 1) {
				// an assert?
				throw new WrappedRuntimeException(
						"Multiple client object loads",
						SuggestedAction.NOTIFY_WARNING);
			}
			DTRSimpleSerialWrapper loadWrapper = loads.iterator().next();
			setClientInstanceIdForGet(loadWrapper.getClientInstanceId());
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
}
