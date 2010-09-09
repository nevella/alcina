package cc.alcina.framework.gwt.gears.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientUIThreadWorker;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.GwtRpcProtocolHandler;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientMetricLogging;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;

public abstract class OfflineDomainLoader {
	public static final String OFFLINE_LOAD_METRIC_KEY = "offline-load";

	private List<DTRSimpleSerialWrapper> transforms;

	public boolean tryOffline(final Throwable t) {
		if (!tryOfflinePass(t, false)) {
			return false;
		}
		if (transforms.isEmpty() && !ClientSession.get().isSoleOpenTab()) {
			// double-check - easier at this level than with a stack of
			// callbacks - well...maybe. easier for me anyway.
			new Timer() {
				@Override
				public void run() {
					tryOfflinePass(t, true);
				}
			}.schedule(ClientSession.KEEP_ALIVE_TIMER + 1000);
		}
		return true;
	}

	private boolean tryOfflinePass(Throwable t, boolean notify) {
		LocalTransformPersistence gears = LocalTransformPersistence.get();
		if (!ClientUtils.maybeOffline(t) || !gears.isLocalStorageInstalled()) {
			return false;
		}
		ClientMetricLogging.get().start(OFFLINE_LOAD_METRIC_KEY);
		PermissionsManager.get().setOnlineState(OnlineState.OFFLINE);
		TransformManager tm = TransformManager.get();
		tm.registerDomainObjectsInHolder(createDummyModel());
		try {
			transforms = gears
					.openAvailableSessionTransformsForOfflineLoad(notify);
			if (!transforms.isEmpty()) {
				if (hasGwtRpcTransforms()) {
					displayReplayRpcNotification(false);
				}
				new Timer() {
					@Override
					public void run() {
						List<DomainTransformEvent> initialEvents = handleGwtRpcTransforms();
						if (loadObjectsHolder != null) {
							registerRpcDomainModelHolder();
						}
						new DTEAsyncDeserializer(transforms, initialEvents)
								.start();
					}
				}.schedule(500);
			}
		} catch (Exception e) {
			ClientMetricLogging.get().end(OFFLINE_LOAD_METRIC_KEY);
			throw new WrappedRuntimeException(e);
		}
		return true;
	}

	protected void registerRpcDomainModelHolder() {
		throw new UnsupportedOperationException();
	}

	protected LoadObjectsHolder<DomainModelHolder> loadObjectsHolder;

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

	/*
	 * at most two gwtrpc wrappers - at the top of the list
	 */
	private List<DomainTransformEvent> handleGwtRpcTransforms() {
		List<DomainTransformEvent> initialEvents = new ArrayList<DomainTransformEvent>();
		for (Iterator<DTRSimpleSerialWrapper> iterator = transforms.iterator(); iterator
				.hasNext();) {
			DTRSimpleSerialWrapper wrapper = iterator.next();
			DTRProtocolHandler handler = new DTRProtocolSerializer()
					.getHandler(wrapper.getProtocolVersion());
			if (handler instanceof GwtRpcProtocolHandler) {
				LoadObjectsHolder replayRpc = replayRpc(wrapper.getText());
				iterator.remove();
				if (replayRpc != null) {
					if (replayRpc.getDomainObjects() != null) {
						loadObjectsHolder = replayRpc;
					}
					initialEvents.addAll(replayRpc.getReplayEvents());
				}
			}
		}
		return initialEvents;
	}

	protected void displayReplayRpcNotification(boolean end) {
	}

	protected LoadObjectsHolder replayRpc(String text) {
		return ((ClientHandshakeHelperWithLocalPersistence) ClientLayerLocator
				.get().getClientHandshakeHelper()).replayRpc(text);
	}

	public abstract DomainModelHolder createDummyModel();

	class DTEAsyncDeserializer extends ClientUIThreadWorker {
		private List<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>();

		DomainTransformRequest persist = null;

		private DTRProtocolHandler protocolHandler;

		private NonCancellableRemoteDialog cd;

		private Iterator<DTRSimpleSerialWrapper> transformIterator;

		private int totalStrlen = 0;

		private int strlenProcessed = 0;

		private DTRSimpleSerialWrapper wr;

		public DTEAsyncDeserializer(List<DTRSimpleSerialWrapper> transforms,
				List<DomainTransformEvent> initialItems) {
			super(1000, 200);
			items = initialItems;
			this.transformIterator = transforms.iterator();
			for (DTRSimpleSerialWrapper wr : transforms) {
				totalStrlen += wr.getText().length();
			}
			allocateToNonWorkerFactor = 0;
		}

		@Override
		public void start() {
			this.cd = new NonCancellableRemoteDialog("") {
				@Override
				protected boolean initialAnimationEnabled() {
					return false;
				}
			};
			cd.setAnimationEnabled(false);
			cd.getGlass().setOpacity(0);
			cd.show();
			super.start();
		}

		@Override
		protected void performIteration() {
			if (protocolHandler == null) {
				wr = transformIterator.next();
				if (wr.getDomainTransformRequestType() == DomainTransformRequestType.TO_REMOTE
						|| wr.getDomainTransformRequestType() == DomainTransformRequestType.TO_REMOTE_COMPLETED) {
					int requestId = (int) wr.getRequestId();
					CommitToStorageTransformListener tl = ClientLayerLocator
							.get().getCommitToStorageTransformListener();
					tl.setLocalRequestId(Math.max(requestId + 1, (int) tl
							.getLocalRequestId()));
					if (wr.getDomainTransformRequestType() == DomainTransformRequestType.TO_REMOTE) {
						persist = new DomainTransformRequest();
						persist.setRequestId(requestId);
						persist.setClientInstance(null);
						persist
								.setDomainTransformRequestType(DomainTransformRequestType.TO_REMOTE);
						persist.setTag(wr.getTag());
						LocalTransformPersistence.get()
								.getPersistedTransforms().put(requestId, wr);
						tl.getPriorRequestsWithoutResponse().add(persist);
					}
				}
				protocolHandler = new DTRProtocolSerializer().getHandler(wr
						.getProtocolVersion());
			}
			int pct = (100 * (strlenProcessed + protocolHandler.getOffset()))
					/ (totalStrlen + 1);
			cd.setStatus("Loading - " + (pct / 2) + "%");
			List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();
			String s = protocolHandler.deserialize(wr.getText(), events,
					iterationCount);
			items.addAll(events);
			if (persist != null) {
				persist.getItems().addAll(events);
			}
			lastPassIterationsPerformed = iterationCount;
			if (s == null) {
				strlenProcessed += protocolHandler.getOffset();
				protocolHandler = null;
			}
			System.out.println("transforms deser:\n" + items.size());
		}

		@Override
		protected boolean isComplete() {
			return protocolHandler == null && !transformIterator.hasNext();
		}

		protected void onComplete() {
			cd.hide();
			new ReplayWorker(items).start();
		}
	}

	class ReplayWorker extends ClientUIThreadWorker {
		private NonCancellableRemoteDialog cd;

		private final List<DomainTransformEvent> items;

		public ReplayWorker(List<DomainTransformEvent> items) {
			this.items = items;
			allocateToNonWorkerFactor = 0;
		}

		@Override
		public void start() {
			this.cd = new NonCancellableRemoteDialog("") {
				@Override
				protected boolean initialAnimationEnabled() {
					return false;
				}
			};
			cd.getGlass().setOpacity(0);
			cd.show();
			MutablePropertyChangeSupport.setMuteAll(true);
			super.start();
		}

		@Override
		protected boolean isComplete() {
			return index == items.size();
		}

		@Override
		protected void onComplete() {
			cd.hide();
			MutablePropertyChangeSupport.setMuteAll(false);
			TransformManager tm = TransformManager.get();
			tm.setReplayingRemoteEvent(true);
			ClientInstance clientInstance = beforeEventReplay();
			CommitToStorageTransformListener tl = ClientLayerLocator.get()
					.getCommitToStorageTransformListener();
			ClientLayerLocator.get().setClientInstance(clientInstance);
			for (DomainTransformRequest rq : tl
					.getPriorRequestsWithoutResponse()) {
				rq.setClientInstance(clientInstance);
				for (DomainTransformEvent dte : rq.getItems()) {
					dte.getObjectClassRef();
				}
			}
			afterEventReplay();
			tm.setReplayingRemoteEvent(false);
		}

		@Override
		protected void performIteration() {
			cd.setStatus("Loading - "
					+ (50 + (index * 50) / (items.size() + 1)) + "%");
			List v1 = new ArrayList();
			lastPassIterationsPerformed = Math.min(iterationCount, items.size()
					- index);
			for (int i = 0; i < lastPassIterationsPerformed; i++) {
				v1.add(items.get(index++));
			}
			TransformManager.get().replayRemoteEvents(v1, false);
			System.out.println("transforms replayed:\n" + index);
		}
	}

	public abstract ClientInstance beforeEventReplay();

	public abstract void afterEventReplay();
}
