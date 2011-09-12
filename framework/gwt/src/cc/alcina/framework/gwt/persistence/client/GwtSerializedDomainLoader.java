package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientUIThreadWorker;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

public abstract class GwtSerializedDomainLoader extends SerializedDomainLoader {
	public GwtSerializedDomainLoader() {
		super();
	}

	protected String getTransformSignature() {
		return GWT.getPermutationStrongName();
	}

	@Override
	protected void replayAfterPossibleDelay() {
		new Timer() {
			@Override
			public void run() {
				replaySequence();
			}
		}.schedule(100);
	}

	@Override
	public void tryOffline(final Throwable t,
			final PersistenceCallback<Boolean> persistenceCallback) {
		final PersistenceCallback<Boolean> secondPassCallback = new PersistenceCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Boolean result) {
				persistenceCallback.onSuccess(result);
			}
		};
		PersistenceCallback<Boolean> firstPassCallback = new PersistenceCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				persistenceCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Boolean result) {
				if (!result) {
					persistenceCallback.onSuccess(false);
				}
				if (transforms.isEmpty()
						&& !ClientSession.get().isSoleOpenTab()) {
					// double-check - easier at this level than with a stack of
					// callbacks - well...maybe. easier for me anyway.
					new Timer() {
						@Override
						public void run() {
							tryOfflinePass(t, true, secondPassCallback);
						}
					}.schedule(ClientSession.KEEP_ALIVE_TIMER + 1000);
				}
			}
		};
		tryOfflinePass(t, false, firstPassCallback);
	}

	protected boolean checkTransformSignature(
			LoadObjectsHolder loadObjectsHolder) {
		return GWT.getPermutationStrongName().equals(
				loadObjectsHolder.getRequest().getTypeSignature());
	}

	protected void replayTransforms(List<DomainTransformEvent> initialEvents) {
		new DTEAsyncDeserializer(transforms, initialEvents).start();
	}

	public class DTEAsyncDeserializer extends ClientUIThreadWorker {
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
		protected boolean isComplete() {
			return protocolHandler == null && !transformIterator.hasNext();
		}

		protected void onComplete() {
			cd.hide();
			new ReplayWorker(items).start();
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
					tl.setLocalRequestId(Math.max(requestId + 1,
							(int) tl.getLocalRequestId()));
					if (wr.getDomainTransformRequestType() == DomainTransformRequestType.TO_REMOTE) {
						persist = new DomainTransformRequest();
						persist.setRequestId(requestId);
						persist.setClientInstance(null);
						persist.setDomainTransformRequestType(DomainTransformRequestType.TO_REMOTE);
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
				persist.getEvents().addAll(events);
			}
			lastPassIterationsPerformed = iterationCount;
			if (s == null) {
				strlenProcessed += protocolHandler.getOffset();
				protocolHandler = null;
			}
			System.out.println("transforms deser:\n" + items.size());
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
			if (clientInstance != null) {
				ClientLayerLocator.get().setClientInstance(clientInstance);
			}
			for (DomainTransformRequest rq : tl
					.getPriorRequestsWithoutResponse()) {
				rq.setClientInstance(clientInstance);
				for (DomainTransformEvent dte : rq.getEvents()) {
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
}