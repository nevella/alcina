/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestTagProvider;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientState;
import cc.alcina.framework.gwt.client.logic.ClientTransformExceptionResolver.ClientTransformExceptionResolutionToken;
import cc.alcina.framework.gwt.client.logic.ClientTransformExceptionResolver.ClientTransformExceptionResolverAction;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.ClientUtils;

/**
 * 
 * @author Nick Reddel
 * 
 *         <h2>Thread-safety notes (applicable to JVM variant
 *         CommitToStorageTransformListenerJvm)</h2>
 *         <ul>
 *         <li>Topic publishers are thread-safe
 *         <li>QueueingFinished timer only accessed in synchronized methods,
 *         ditto localRequestId ditto committingRequest
 *         </ul>
 * 
 * 
 */
public class CommitToStorageTransformListener
		implements DomainTransformListener {
	public static final int DELAY_MS = 100;

	public static final transient String CONTEXT_REPLAYING_SYNTHESISED_EVENTS = CommitToStorageTransformListener.class
			.getName() + ".CONTEXT_REPLAYING_SYNTHESISED_EVENTS";

	public static final transient String CONTEXT_COMMITTING_REQUEST = CommitToStorageTransformListener.class
			.getName() + ".CONTEXT_COMMITTING_REQUEST";

	private static final String TOPIC_DOMAIN_EXCEPTION = CommitToStorageTransformListener.class
			.getName() + ".TOPIC_DOMAIN_EXCEPTION";

	private static final String TOPIC_TRANSFORMS_COMMITTED = CommitToStorageTransformListener.class
			.getName() + ".TOPIC_TRANSFORMS_COMMITTED";

	public static void flushAndRun(Runnable runnable) {
		Registry.impl(CommitToStorageTransformListener.class)
				.flushWithOneoffCallback(new AsyncCallbackStd() {
					@Override
					public void onSuccess(Object result) {
						runnable.run();
					}
				});
	}

	public static void
			flushAndRunWithCreationConsumer(Consumer<EntityLocator> r2) {
		flushAndRun(() -> r2.accept(CommitToStorageTransformListener
				.get().lastCreatedObjectLocator));
	}

	public static void
			flushAndRunWithFirstCreationConsumer(Consumer<EntityLocator> r2) {
		flushAndRun(() -> r2.accept(CommitToStorageTransformListener
				.get().firstCreatedObjectLocator));
	}

	public static CommitToStorageTransformListener get() {
		return Registry.impl(CommitToStorageTransformListener.class);
	}

	public static void notifyCommitDomainExceptionListenerDelta(
			TopicListener<Throwable> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_DOMAIN_EXCEPTION,
				listener, add);
	}

	public static Topic<DomainTransformResponse> topicTransformsCommitted() {
		return Topic.global(TOPIC_TRANSFORMS_COMMITTED);
	}

	static void notifyCommitDomainException(Throwable message) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_DOMAIN_EXCEPTION,
				message);
	}

	private Topic<State> topicStateChanged = Topic.local();

	protected Object collectionsMonitor = new Object();

	private EntityLocator lastCreatedObjectLocator;

	private EntityLocator firstCreatedObjectLocator;

	protected List<DomainTransformEvent> transformQueue = new ArrayList<>();

	protected List<DomainTransformRequest> priorRequestsWithoutResponse = new ArrayList<>();

	protected TimerWrapper queueingFinishedTimer;

	protected long lastQueueAddMillis;

	private boolean suppressErrors = false;

	private boolean paused;

	private int localRequestId = 1;

	private List<DomainTransformEvent> synthesisedEvents = new ArrayList<>();

	private boolean reloadRequired = false;

	protected Set<Long> eventIdsToIgnore = new HashSet<Long>();

	private State currentState;

	private boolean localStorageOnly;

	private int lastCommitSize;

	private boolean queueCommitTimerDisabled;

	private Object commitMonitor = new Object();

	private TopicListener<State> stateListener = (k, v) -> currentState = v;

	public CommitToStorageTransformListener() {
		if (GWT.isClient()) {
			new WindowClosingHandler().add();
		}
		topicStateChanged.add(stateListener);
	}

	public void clearPriorRequestsWithoutResponse() {
		synchronized (collectionsMonitor) {
			priorRequestsWithoutResponse.clear();
		}
	}

	@Override
	public void domainTransform(DomainTransformEvent evt) {
		if (evt.getCommitType() == CommitType.TO_STORAGE) {
			if (Objects.equals(evt.getPropertyName(),
					TransformManager.ID_FIELD_NAME)) {
				return;
			}
			TransformManager tm = TransformManager.get();
			if (tm.isReplayingRemoteEvent()) {
				return;
			}
			synchronized (collectionsMonitor) {
				transformQueue.add(evt);
			}
			lastQueueAddMillis = System.currentTimeMillis();
			if (queueingFinishedTimer == null
					&& !isQueueCommitTimerDisabled()) {
				queueingFinishedTimer = Registry
						.impl(TimerWrapperProvider.class)
						.getTimer(getCommitLoopRunnable());
				queueingFinishedTimer.scheduleRepeating(DELAY_MS);
			}
			return;
		}
	}

	public synchronized void flush() {
		if (currentState == State.RELOAD) {
			return;
		}
		commit();
	}

	public void flushWithOneoffCallback(AsyncCallback callback) {
		flushWithOneoffCallback(callback, true);
	}

	public void flushWithOneoffCallback(AsyncCallback callback,
			boolean commitIfEmptyTransformQueue) {
		boolean doNotFlush = false;
		synchronized (collectionsMonitor) {
			doNotFlush = ((priorRequestsWithoutResponse.size() == 0
					|| !commitIfEmptyTransformQueue)
					&& transformQueue.size() == 0) || isPaused();
		}
		if (doNotFlush) {
			callback.onSuccess(null);
			return;
		}
		topicStateChanged().add(new OneoffListenerWrapper(callback));
		flush();
	}

	public State getCurrentState() {
		return this.currentState;
	}

	public int getLastCommitSize() {
		return this.lastCommitSize;
	}

	public int getLocalRequestId() {
		return this.localRequestId;
	}

	public List<DomainTransformRequest> getPriorRequestsWithoutResponse() {
		synchronized (collectionsMonitor) {
			return Collections
					.unmodifiableList(this.priorRequestsWithoutResponse.stream()
							.collect(Collectors.toList()));
		}
	}

	public List<DomainTransformEvent> getSynthesisedEvents() {
		synchronized (collectionsMonitor) {
			return Collections.unmodifiableList(this.synthesisedEvents.stream()
					.collect(Collectors.toList()));
		}
	}

	public int getTransformQueueSize() {
		synchronized (collectionsMonitor) {
			return transformQueue.size();
		}
	}

	/*
	 * Should pretty much always be false. If a transform fails, there are
	 * consistency issues up and down the line - best to reload and try again
	 */
	public boolean isAllowPartialRetryRequests() {
		return false;
	}

	/*
	 * vaguely hacky, if we're connected but need to do some fancy footwork
	 * before uploading offline transforms
	 */
	public boolean isLocalStorageOnly() {
		return this.localStorageOnly;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isQueueCommitTimerDisabled() {
		return this.queueCommitTimerDisabled;
	}

	public boolean isSuppressErrors() {
		return suppressErrors;
	}

	public void setLocalRequestId(int localRequestId) {
		this.localRequestId = localRequestId;
	}

	public void setLocalStorageOnly(boolean localStorageOnly) {
		this.localStorageOnly = localStorageOnly;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public void setQueueCommitTimerDisabled(boolean queueCommitTimerDisabled) {
		this.queueCommitTimerDisabled = queueCommitTimerDisabled;
	}

	public void setSuppressErrors(boolean suppressErrors) {
		this.suppressErrors = suppressErrors;
	}

	public Topic<State> topicStateChanged() {
		return this.topicStateChanged;
	}

	protected boolean canTransitionToOnline() {
		return true;
	}

	protected void commit() {
		synchronized (collectionsMonitor) {
			if ((priorRequestsWithoutResponse.size() == 0
					&& transformQueue.size() == 0) || isPaused()) {
				return;
			}
		}
		synchronized (commitMonitor) {
			commit0();
		}
	}

	protected synchronized void commit0() {
		if (queueingFinishedTimer != null) {
			queueingFinishedTimer.cancel();
		}
		queueingFinishedTimer = null;
		int requestId = localRequestId++;
		final DomainTransformRequest request = DomainTransformRequest
				.createPersistableRequest(requestId,
						PermissionsManager.get().getClientInstanceId());
		request.setRequestId(requestId);
		request.setClientInstance(PermissionsManager.get().getClientInstance());
		synchronized (collectionsMonitor) {
			customProcessTransformQueue(transformQueue);
			request.getEvents().addAll(transformQueue);
			lastCommitSize = transformQueue.size();
			request.getEventIdsToIgnore().addAll(eventIdsToIgnore);
			request.setTag(DomainTransformRequestTagProvider.get().getTag());
			updateTransformQueueVersions();
			transformQueue.clear();
		}
		if(request.getEvents().isEmpty()){
			return;
		}
		final AsyncCallback<DomainTransformResponse> commitRemoteCallback = new ResponseCallback(
				request);
		// the ordering here is tricky
		// 'committing' says 'we have a flat list of rqs without response (incl
		// current)
		// wheras the transform rpc call requires rq (current) with prior rqs as
		// a listfield
		// given listener callbacks can be multi-threaded (jvm version),
		// use the following ordering - note we use a new list in
		// dtr.setPriorRequestsWithoutResponse(priorRequestsWithoutResponseCopy)
		synchronized (collectionsMonitor) {
			List<DomainTransformRequest> priorRequestsWithoutResponseForCommit = new ArrayList<DomainTransformRequest>(
					priorRequestsWithoutResponse);
			request.setPriorRequestsWithoutResponse(
					priorRequestsWithoutResponseForCommit);
		}
		try {
			LooseContext.pushWithKey(CONTEXT_COMMITTING_REQUEST, request);
			topicStateChanged().publish(State.COMMITTING);
		} finally {
			LooseContext.pop();
		}
		synchronized (collectionsMonitor) {
			priorRequestsWithoutResponse.add(request);
		}
		if (isLocalStorageOnly()) {
			topicStateChanged().publish(State.OFFLINE);
			return;
		}
		if (PermissionsManager.get().getOnlineState() == OnlineState.OFFLINE) {
			Client.commonRemoteService().ping(new AsyncCallback<Void>() {
				@Override
				public void onFailure(Throwable caught) {
					// ignore - expected(ish) behaviour - if it's a
					// non-'offline' error, it's more graceful to ignore
					// here than not
					topicStateChanged().publish(State.OFFLINE);
				}

				@Override
				public void onSuccess(Void result) {
					if (canTransitionToOnline()) {
						commitRemote(request, commitRemoteCallback);
					}
				}
			});
		} else {
			commitRemote(request, commitRemoteCallback);
		}
	}

	protected void commitRemote(DomainTransformRequest request,
			AsyncCallback<DomainTransformResponse> callback) {
		Client.commonRemoteService().transform(request, callback);
	}

	protected void customProcessTransformQueue(
			List<DomainTransformEvent> transformQueue) {
		// for outré subclassed persistence
	}

	protected Runnable getCommitLoopRunnable() {
		return new CommitLoopRunnable();
	}

	protected int getMaxTransformsPerRequest() {
		return Integer.MAX_VALUE;
	}

	protected ClientTransformExceptionResolver getTransformExceptionResolver() {
		return Registry.impl(ClientTransformExceptionResolver.class);
	}

	protected List<DomainTransformEvent> getTransformQueue() {
		return this.transformQueue;
	}

	/*
	 * Unimplemented for the moment. This may or may not be necessary to
	 * accelerate change conflict checking
	 */
	void updateTransformQueueVersions() {
	}

	public static enum State {
		COMMITTING, COMMITTED, ERROR, OFFLINE, RELOAD
	}

	public static class UnknownTransformFailedException
			extends WrappedRuntimeException {
		public UnknownTransformFailedException(Throwable cause) {
			super(cause);
		}
	}

	private class ResponseCallback
			implements AsyncCallback<DomainTransformResponse> {
		private final DomainTransformRequest request;

		private ResponseCallback(DomainTransformRequest request) {
			this.request = request;
		}

		@Override
		public void onFailure(Throwable caught) {
			// resolve here
			if (!suppressErrors) {
				if (caught instanceof DomainTransformRequestException) {
					final DomainTransformRequestException dtre = (DomainTransformRequestException) caught;
					Callback<ClientTransformExceptionResolutionToken> callback = new Callback<ClientTransformExceptionResolutionToken>() {
						@Override
						public void apply(
								ClientTransformExceptionResolutionToken resolutionToken) {
							if (resolutionToken
									.getResolverAction() == ClientTransformExceptionResolverAction.RESUBMIT) {
								synchronized (collectionsMonitor) {
									eventIdsToIgnore = resolutionToken
											.getEventIdsToIgnore();
								}
								reloadRequired = resolutionToken
										.isReloadRequired();
								setPaused(false);
								commit();
								return;
							} else {
								throw new WrappedRuntimeException(dtre,
										SuggestedAction.RELOAD);
							}
						}
					};
					setPaused(true);
					if (!ClientState.get().isAppReadOnly()) {
						getTransformExceptionResolver().resolve(dtre, callback);
					}
					return;
				}
				if (ClientUtils.maybeOffline(caught)) {
					topicStateChanged().publish(State.OFFLINE);
				}
				throw new UnknownTransformFailedException(caught);
			}
			notifyCommitDomainException(caught);
			topicStateChanged().publish(State.ERROR);
		}

		@Override
		public void onSuccess(DomainTransformResponse response) {
			try {
				LooseContext.pushWithTrue(
						CommitToStorageTransformListener.CONTEXT_REPLAYING_SYNTHESISED_EVENTS);
				onSuccess0(response);
			} finally {
				LooseContext.pop();
			}
		}

		// must be synchronized for multithreaded (RCP) clients - note,
		// better yet would be to pass synthesisedEvents with the emitted
		// callbacks/events
		private void onSuccess0(DomainTransformResponse response) {
			synchronized (CommitToStorageTransformListener.this) {
				PermissionsManager.get().setOnlineState(OnlineState.ONLINE);
				TransformManager tm = TransformManager.get();
				tm.setReplayingRemoteEvent(true);
				List<DomainTransformEvent> synthesisedEvents = new ArrayList<>();
				try {
					lastCreatedObjectLocator = null;
					firstCreatedObjectLocator = null;
					/*
					 * either way we do this (server or client), it's going to
					 * seem a bit hacky but...a client's interpretation of what
					 * is the canonical event (e.g. createObject on the server)
					 * is more its business than the TLTM's
					 * 
					 * so...leave here. for now
					 */
					for (DomainTransformEvent dte : response
							.getEventsToUseForClientUpdate()) {
						long id = dte.getGeneratedServerId() != 0
								? dte.getGeneratedServerId()
								: dte.getObjectId();
						if (dte.getGeneratedServerId() != 0) {
							DomainTransformEvent idEvt = new DomainTransformEvent();
							idEvt.setObjectClass(dte.getObjectClass());
							idEvt.setObjectLocalId(dte.getObjectLocalId());
							idEvt.setPropertyName(
									TransformManager.ID_FIELD_NAME);
							idEvt.setValueClass(Long.class);
							idEvt.setTransformType(
									TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
							idEvt.setNewStringValue(String.valueOf(id));
							synthesisedEvents.add(idEvt);
							EntityLocator entityLocator = new EntityLocator(
									dte.getObjectClass(), id,
									dte.getObjectLocalId());
							if (firstCreatedObjectLocator == null) {
								firstCreatedObjectLocator = entityLocator;
							}
							lastCreatedObjectLocator = entityLocator;
						}
						// if (dte.getObjectVersionNumber() != 0 && id != 0)
						// {
						// if we have zero id at this stage, we're probably
						// in a
						// race condition
						// with some other persistence mech
						// and it definitely _does_ need to be sorted
						if (CommonUtils.iv(dte.getObjectVersionNumber()) != 0) {
							DomainTransformEvent idEvt = new DomainTransformEvent();
							idEvt.setObjectClass(dte.getObjectClass());
							idEvt.setObjectId(id);
							idEvt.setPropertyName(
									TransformManager.VERSION_FIELD_NAME);
							idEvt.setNewStringValue(String
									.valueOf(dte.getObjectVersionNumber()));
							idEvt.setValueClass(Integer.class);
							idEvt.setTransformType(
									TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
							synthesisedEvents.add(idEvt);
						}
					}
					for (DomainTransformEvent dte : synthesisedEvents) {
						try {
							tm.apply(dte);
							tm.fireDomainTransform(dte);//
							// this notifies storage (for offline replay)
							//
							// also notifies clients who need to
							// know chanages were committed
						} catch (DomainTransformException e) {
							// shouldn't happen, if the server code's ok
							// note - squelching (for the moment)
							// sourceentitynotfound -
							// assume object was either deregistered or
							// deleted
							// before these transforms
							// made it back from the server
							// actually, e.g. deletion - there'll be a
							// version
							// change which gets propagated back
							// more correct would be to record deleted
							// objects...but it don't matter much
							if (e.getType() == DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND
									|| e.getType() == DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND) {
							} else {
								throw new WrappedRuntimeException(e);
							}
						}
					}
					List<DomainTransformEvent> items = this.request.getEvents();
					for (DomainTransformEvent evt : items) {
						TransformManager.get().setTransformCommitType(evt,
								CommitType.ALL_COMMITTED);
					}
					for (int i = priorRequestsWithoutResponse.size()
							- 1; i >= 0; i--) {
						if (priorRequestsWithoutResponse.get(i)
								.getRequestId() <= this.request
										.getRequestId()) {
							priorRequestsWithoutResponse.remove(i);
						}
					}
					if (response.getMessage() != null) {
						Registry.impl(ClientNotifications.class)
								.showMessage(response.getMessage());
					}
					synchronized (collectionsMonitor) {
						CommitToStorageTransformListener.this.synthesisedEvents
								.clear();
						CommitToStorageTransformListener.this.synthesisedEvents
								.addAll(synthesisedEvents);
					}
				} finally {
					tm.setReplayingRemoteEvent(false);
					if (reloadRequired) {
						topicStateChanged().publish(State.RELOAD);
					} else {
						topicStateChanged().publish(State.COMMITTED);
						topicTransformsCommitted().publish(response);
					}
				}
			}
		}
	}

	private static class WindowClosingHandler {
		public void add() {
			Window.addWindowClosingHandler(evt -> {
				CommitToStorageTransformListener transformListener = CommitToStorageTransformListener
						.get();
				transformListener.setPaused(false);
				transformListener.flush();
			});
		}
	}

	class CommitLoopRunnable implements Runnable {
		long checkMillis = lastQueueAddMillis;

		@Override
		public void run() {
			if (checkMillis == lastQueueAddMillis
					|| transformQueue.size() > getMaxTransformsPerRequest()) {
				commit();
			}
			checkMillis = lastQueueAddMillis;
		}
	}

	class OneoffListenerWrapper implements TopicListener<State> {
		private final AsyncCallback callback;

		public OneoffListenerWrapper(AsyncCallback callback) {
			this.callback = callback;
		}

		@Override
		public void topicPublished(String key, State state) {
			switch (state) {
			case COMMITTING:
				return;
			case COMMITTED:
			case OFFLINE:
				topicStateChanged().remove(this);
				if (!reloadRequired) {
					callback.onSuccess(null);
				}
				break;
			}
			topicStateChanged().remove(this);
			if (!reloadRequired) {
				callback.onFailure(new Exception("flush failed on server"));
			}
		}
	}
}