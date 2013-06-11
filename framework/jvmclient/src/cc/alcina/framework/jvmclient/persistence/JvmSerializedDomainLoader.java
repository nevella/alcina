package cc.alcina.framework.jvmclient.persistence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.LoadObjectsHolder;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.SerializedDomainLoader;

import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class JvmSerializedDomainLoader extends SerializedDomainLoader {
	@Override
	public void tryOffline(final Throwable t,
			final AsyncCallback<Boolean> AsyncCallback) {
		AsyncCallback<Boolean> firstPassCallback = new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				AsyncCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Boolean result) {
				AsyncCallback.onSuccess(result);
			}
		};
		tryOfflinePass(t, false, firstPassCallback);
	}

	@Override
	protected boolean shouldTryOffline(Throwable t,
			LocalTransformPersistence localPersistence) {
		return true;
	}

	protected boolean checkTransformSignature(LoadObjectsHolder replayRpc) {
		return true;
	}

	protected void replayTransforms(List<DomainTransformEvent> initialEvents) {
		new DTEDeserializer(transforms, initialEvents).run();
	}

	protected void addRemoteNoLocalYesDtrToStorageQueue(
			DomainTransformRequest persist) {
		CommitToStorageTransformListener tl = ClientLayerLocator.get()
				.getCommitToStorageTransformListener();
		tl.getPriorRequestsWithoutResponse().add(persist);
	}

	public class DTEDeserializer {
		private List<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>();

		DomainTransformRequest persist = null;

		private DTRProtocolHandler protocolHandler;

		private Iterator<DTRSimpleSerialWrapper> transformIterator;

		@SuppressWarnings("unused")
		private int strlenProcessed = 0;

		private DTRSimpleSerialWrapper wr;

		public DTEDeserializer(List<DTRSimpleSerialWrapper> transforms,
				List<DomainTransformEvent> initialItems) {
			items = initialItems;
			this.transformIterator = transforms.iterator();
		}

		private void finishDeserialize() {
			MutablePropertyChangeSupport.setMuteAll(false, true);
			TransformManager tm = TransformManager.get();
			ClientInstance clientInstance = beforeEventReplay();
			tm.setReplayingRemoteEvent(true);
			CommitToStorageTransformListener tl = ClientLayerLocator.get()
					.getCommitToStorageTransformListener();
			Registry.impl(HandshakeConsortModel.class).setClientInstance(
					clientInstance);
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
						addRemoteNoLocalYesDtrToStorageQueue(persist);
					}
				}
				protocolHandler = new DTRProtocolSerializer().getHandler(wr
						.getProtocolVersion());
			}
			List<DomainTransformEvent> events = new ArrayList<DomainTransformEvent>();
			String s = protocolHandler.deserialize(wr.getText(), events,
					Integer.MAX_VALUE);
			items.addAll(events);
			if (persist != null) {
				persist.getEvents().addAll(events);
			}
			if (s == null) {
				strlenProcessed += protocolHandler.getOffset();
				protocolHandler = null;
			}
			System.out.println("transforms deser:\n" + items.size());
		}

		void run() {
			MutablePropertyChangeSupport.setMuteAll(true, true);
			while (transformIterator.hasNext()) {
				performIteration();
			}
			TransformManager.get().replayRemoteEvents(items, false);
			finishDeserialize();
		}
	}
}
