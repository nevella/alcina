package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientUIThreadWorker;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRSimpleSerialSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.GwtRpcProtocolHandler;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.Lzw;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

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
 * <li><b>this is because there may have been a persist (sync) before persist of
 * initial (async) - better to order the rq by request, then db id</b>
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
	public static final String CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED = LocalTransformPersistence.class
			.getName() + "." + "CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED";

	public static final String CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED_CLIENT_IDS = LocalTransformPersistence.class
			.getName()
			+ "."
			+ "CONTEXT_OFFLINE_TRANSFORM_UPLOAD_SUCCEEDED_CLIENT_IDS";;

	public static final String TOPIC_PERSISTING = LocalTransformPersistence.class
			.getName() + "." + "TOPIC_PERSISTING";;

	public static class TypeSizeTuple {
		public String type;

		public TypeSizeTuple(String type, int size) {
			this.type = type;
			this.size = size;
		}

		public int size;
	}

	public static void notifyPersisting(TypeSizeTuple size) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_PERSISTING, size);
	}

	public static void notifyPersistingListenerDelta(
			TopicListener<TypeSizeTuple> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_PERSISTING, listener,
				add);
	}

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

	private boolean useLzw;

	public LocalTransformPersistence() {
	}

	public abstract void clearPersistedClient(ClientInstance exceptFor,
			int exceptForId, AsyncCallback callback);

	public void dumpDatabase(final Callback<String> callback) {
		AsyncCallback<List<DTRSimpleSerialWrapper>> transformCallback = new AsyncCallbackStd<List<DTRSimpleSerialWrapper>>() {
			@Override
			public void onSuccess(List<DTRSimpleSerialWrapper> result) {
				StringBuilder sb = new StringBuilder();
				for (DTRSimpleSerialWrapper wrapper : result) {
					sb.append(new DTRSimpleSerialSerializer().write(wrapper));
					sb.append("\n");
				}
				callback.apply(sb.toString());
			}
		};
		getTransforms(DomainTransformRequestType.values(), transformCallback);
	}

	public CommitToStorageTransformListener getCommitToStorageTransformListener() {
		return commitToStorageTransformListener;
	}

	public Map<Integer, DTRSimpleSerialWrapper> getPersistedTransforms() {
		return this.persistedTransforms;
	}

	public abstract String getPersistenceStoreName();

	public DTESerializationPolicy getSerializationPolicy() {
		return serializationPolicy;
	}

	public void handleUncommittedTransformsOnLoad(final AsyncCallback<Void> cb) {
		if (!isLocalStorageInstalled()) {
			cb.onSuccess(null);
			return;
		}
		new UploadOfflineTransformsConsort(cb).start();
	}

	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			AsyncCallback<Void> callback) {
		setSerializationPolicy(dteSerializationPolicy);
		setCommitToStorageTransformListener(commitToServerTransformListener);
		callback.onSuccess(null);
	}

	public boolean isClosing() {
		return closing;
	}

	public static boolean isLocalStorageInstalled() {
		return get() != null && get().localStorageInstalled;
	}

	/**
	 * Note - this will be a performance problem for large (1mb+) blobs on
	 * iDevices
	 */
	public boolean isUseLzw() {
		return this.useLzw;
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
				persist(wrapper, AsyncCallbackStd.VOID_CALLBACK);
			}
		}
	}

	public void persistInitialRpcPayload(String payload,
			AsyncCallback<Void> AsyncCallback) {
		ClientInstance clientInstance = ClientLayerLocator.get()
				.getClientInstance();
		DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(0, payload,
				System.currentTimeMillis(), PermissionsManager.get()
						.getUserId(), clientInstance.getId(), 0,
				clientInstance.getAuth(),
				DomainTransformRequestType.CLIENT_OBJECT_LOAD,
				GwtRpcProtocolHandler.VERSION, "");
		persist(wrapper, AsyncCallback);
	}

	public abstract void reparentToClientInstance(
			DTRSimpleSerialWrapper wrapper, ClientInstance clientInstance,
			AsyncCallback callback);

	public void restoreDatabase(String data, Callback callback) {
		new Loader(this, data, callback).start();
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

	public void setUseLzw(boolean useLzw) {
		this.useLzw = useLzw;
	}

	public boolean shouldPersistClient(boolean clientSupportsRpcPersistence) {
		return !ClientSession.get().isInitialObjectsPersisted()
				|| clientSupportsRpcPersistence;
	}

	public void stateChanged(Object source, String newState) {
		if (newState == CommitToStorageTransformListener.COMMITTING) {
			DomainTransformRequest rq = getCommitToStorageTransformListener()
					.getCommittingRequest();
			final int requestId = rq.getRequestId();
			if (!getPersistedTransforms().containsKey(requestId)
					&& !rq.getEvents().isEmpty()) {
				rq.setProtocolVersion(getSerializationPolicy()
						.getTransformPersistenceProtocol());
				final DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
						rq);
				getPersistedTransforms().put(requestId, wrapper);
				persist(wrapper, new AsyncCallbackStd<Void>() {
					@Override
					public void onFailure(Throwable caught) {
						getPersistedTransforms().remove(requestId);
						super.onFailure(caught);
					}

					@Override
					public void onSuccess(Void result) {
					}
				});
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
			AsyncCallback<Void> afterTransformsMarkedAsPersistedCallback = new AsyncCallback<Void>() {
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
					persist(wrapper, AsyncCallbackStd.VOID_CALLBACK);
				}
			};
			transformPersisted(persistedWrappers,
					afterTransformsMarkedAsPersistedCallback);
			return;
		} else if (newState == CommitToStorageTransformListener.RELOAD) {
			clearAllPersisted(AsyncCallbackStd.VOID_CALLBACK);
		}
	}

	protected abstract void clearAllPersisted(AsyncCallback callback);

	protected Long getClientInstanceIdForGet() {
		return clientInstanceIdForGet;
	}

	protected void getTransforms(DomainTransformRequestType type,
			AsyncCallback<List<DTRSimpleSerialWrapper>> callback) {
		getTransforms(new DomainTransformRequestType[] { type }, callback);
	}

	protected abstract void getTransforms(DomainTransformRequestType[] types,
			AsyncCallback<List<DTRSimpleSerialWrapper>> callback);

	private static final String LZW_PROTOCOL_ADDITION = "/lzw";

	protected void maybeCompressWrapper(DTRSimpleSerialWrapper wrapper) {
		if (isUseLzw()
				&& wrapper.getProtocolVersion() != null
				&& !wrapper.getProtocolVersion()
						.endsWith(LZW_PROTOCOL_ADDITION)) {
			wrapper.setText(new Lzw().compress(wrapper.getText()));
			wrapper.setProtocolVersion(wrapper.getProtocolVersion()
					+ LZW_PROTOCOL_ADDITION);
		}
	}

	protected void maybeDecompressWrapper(DTRSimpleSerialWrapper wrapper) {
		if (wrapper.getProtocolVersion().endsWith(LZW_PROTOCOL_ADDITION)) {
			wrapper.setText(new Lzw().decompress(wrapper.getText()));
		}
	}

	protected abstract void persist(DTRSimpleSerialWrapper wrapper,
			AsyncCallback callback);

	protected void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted, ModalNotifier notifier,
			AsyncCallback<Void> postPersistOfflineTransformsCallback) {
		ClientLayerLocator
				.get()
				.commonRemoteServiceAsyncInstance()
				.persistOfflineTransforms(uncommitted,
						postPersistOfflineTransformsCallback);
	}

	protected void setClientInstanceIdForGet(Long clientInstanceIdForGet) {
		this.clientInstanceIdForGet = clientInstanceIdForGet;
	}

	protected void setLocalStorageInstalled(boolean localStorageInstalled) {
		this.localStorageInstalled = localStorageInstalled;
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
			AsyncCallback callback);

	ClientInstance getDomainObjectsPersistedBy() {
		return this.domainObjectsPersistedBy;
	}

	private static class Loader {
		private final Callback callback;;

		private final String data;

		private Phase phase = Phase.CLEAR;

		private final LocalTransformPersistence localTransformPersistence;

		AsyncCallbackStd pcb = new AsyncCallbackStd() {
			@Override
			public void onSuccess(Object result) {
				iterate();
			}
		};

		Iterator<DTRSimpleSerialWrapper> loadIterator;

		public Loader(LocalTransformPersistence localTransformPersistence,
				String data, Callback callback) {
			this.localTransformPersistence = localTransformPersistence;
			this.data = data;
			this.callback = callback;
		}

		public void start() {
			List<DTRSimpleSerialWrapper> wrappers = new DTRSimpleSerialSerializer()
					.readMultiple(data);
			loadIterator = wrappers.iterator();
			iterate();
		}

		private void iterate() {
			if (phase == Phase.CLEAR) {
				phase = Phase.LOAD;
				localTransformPersistence.clearAllPersisted(pcb);
			} else {
				if (loadIterator.hasNext()) {
					localTransformPersistence.persist(loadIterator.next(), pcb);
				} else {
					callback.apply(null);
				}
			}
		}

		private enum Phase {
			CLEAR, LOAD
		}
	}

	protected class DTRAsyncSerializer extends ClientUIThreadWorker {
		DTRSimpleSerialWrapper wrapper;

		StringBuffer sb = new StringBuffer();

		private List<DomainTransformEvent> items;

		DTRProtocolHandler handler;

		public DTRAsyncSerializer(DomainTransformRequest dtr) {
			super(1000, 200);
			wrapper = new DTRSimpleSerialWrapper(dtr, true);
			items = dtr.getEvents();
			handler = new DTRProtocolSerializer()
					.getHandler(getSerializationPolicy()
							.getTransformPersistenceProtocol());
		}

		@Override
		protected boolean isComplete() {
			return index == items.size();
		}

		protected void onComplete() {
			ClientLayerLocator.get().notifications().metricLogStart("persist");
			sb = handler.finishSerialization(sb);
			wrapper.setText(sb.toString());
			persist(wrapper, new AsyncCallbackStd() {
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
			for (; index < max; index++) {
				handler.appendTo(items.get(index), sb2);
			}
			sb.append(sb2.toString());
		}
	}

	public abstract void getDomainModelDeltaIterator(
			DomainTransformRequestType[] types,
			AsyncCallback<Iterator<DomainModelDelta>> callback);
}
