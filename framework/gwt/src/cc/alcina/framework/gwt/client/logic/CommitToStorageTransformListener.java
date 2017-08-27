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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.StateListenable;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestTagProvider;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.ClientTransformExceptionResolver.ClientTransformExceptionResolutionToken;
import cc.alcina.framework.gwt.client.logic.ClientTransformExceptionResolver.ClientTransformExceptionResolverAction;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.ClientUtils;

/**
 * 
 * @author Nick Reddel
 */
public class CommitToStorageTransformListener extends StateListenable
        implements DomainTransformListener {
    public static final int DELAY_MS = 100;

    public static final String COMMITTING = "COMMITTING";

    public static final String COMMITTED = "COMMITTED";

    public static final String ERROR = "ERROR";

    public static final String OFFLINE = "OFFLINE";

    public static final String RELOAD = "RELOAD";

    public static final transient String CONTEXT_REPLAYING_SYNTHESISED_EVENTS = CommitToStorageTransformListener.class
            .getName() + ".CONTEXT_REPLAYING_SYNTHESISED_EVENTS";

    private List<DomainTransformEvent> transformQueue;

    private List<DomainTransformRequest> priorRequestsWithoutResponse = new ArrayList<DomainTransformRequest>();

    protected TimerWrapper queueingFinishedTimer;

    protected long lastQueueAddMillis;

    private boolean suppressErrors = false;

    private boolean paused;

    private int localRequestId = 1;

    private Map<Long, Long> localToServerIds = new HashMap<Long, Long>();

    ArrayList<DomainTransformEvent> synthesisedEvents;

    private boolean reloadRequired = false;

    private Set<Long> eventIdsToIgnore = new HashSet<Long>();

    private String currentState;

    private DomainTransformRequest committingRequest;

    private boolean localStorageOnly;

    public CommitToStorageTransformListener() {
        resetQueue();
    }

    public synchronized void domainTransform(DomainTransformEvent evt) {
        if (evt.getCommitType() == CommitType.TO_STORAGE) {
            String pn = evt.getPropertyName();
            if (pn != null && (pn.equals(TransformManager.ID_FIELD_NAME)
                    || pn.equals(TransformManager.LOCAL_ID_FIELD_NAME))) {
                return;
            }
            TransformManager tm = TransformManager.get();
            if (tm.isReplayingRemoteEvent()) {
                return;
            }
            transformQueue.add(evt);
            lastQueueAddMillis = System.currentTimeMillis();
            if (queueingFinishedTimer == null) {
                queueingFinishedTimer = Registry
                        .impl(TimerWrapperProvider.class)
                        .getTimer(getCommitLoopRunnable());
                queueingFinishedTimer.scheduleRepeating(DELAY_MS);
            }
            return;
        }
    }

    public void flush() {
        if (currentState == RELOAD) {
            return;
        }
        commit();
    }

    public void flushWithOneoffCallback(AsyncCallback callback) {
        flushWithOneoffCallback(callback, true);
    }

    public void flushWithOneoffCallback(AsyncCallback callback,
            boolean commitIfEmptyTransformQueue) {
        if (((priorRequestsWithoutResponse.size() == 0
                || !commitIfEmptyTransformQueue) && transformQueue.size() == 0)
                || isPaused()) {
            callback.onSuccess(null);
            return;
        }
        addStateChangeListener(new OneoffListenerWrapper(callback));
        flush();
    }

    public DomainTransformRequest getCommittingRequest() {
        return this.committingRequest;
    }

    public String getCurrentState() {
        return this.currentState;
    }

    public int getLocalRequestId() {
        return this.localRequestId;
    }

    public List<DomainTransformRequest> getPriorRequestsWithoutResponse() {
        return this.priorRequestsWithoutResponse;
    }

    public ArrayList<DomainTransformEvent> getSynthesisedEvents() {
        return this.synthesisedEvents;
    }

    public int getTransformQueueSize() {
        return transformQueue.size();
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

    public boolean isSuppressErrors() {
        return suppressErrors;
    }

    public Long localToServerId(Long localId) {
        return localToServerIds.get(localId);
    }

    /**
     * Indicates that no further transforms should be processed
     */
    public void putReloadRequired() {
        currentState = RELOAD;
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

    public void setSuppressErrors(boolean suppressErrors) {
        this.suppressErrors = suppressErrors;
    }

    private ClientInstance getClientInstance() {
        ClientInstance clientInstance = ClientBase.getClientInstance().clone();
        clientInstance.setUser(null);
        return clientInstance;
    }

    private synchronized void resetQueue() {
        transformQueue = new ArrayList<DomainTransformEvent>();
        // eventIdsToIgnore = new HashSet<Long>();
    }

    protected boolean canTransitionToOnline() {
        return true;
    }

    protected void clearPriorRequestsWithoutResponse() {
        priorRequestsWithoutResponse.clear();
    }

    protected synchronized void commit() {
        if ((priorRequestsWithoutResponse.size() == 0
                && transformQueue.size() == 0) || isPaused()) {
            return;
        }
        if (queueingFinishedTimer != null) {
            queueingFinishedTimer.cancel();
        }
        queueingFinishedTimer = null;
        final DomainTransformRequest dtr = new DomainTransformRequest();
        dtr.setRequestId(localRequestId++);
        dtr.setClientInstance(getClientInstance());
        dtr.getEvents().addAll(transformQueue);
        dtr.getEventIdsToIgnore().addAll(eventIdsToIgnore);
        dtr.setTag(DomainTransformRequestTagProvider.get().getTag());
        updateTransformQueueVersions();
        resetQueue();
        final AsyncCallback<DomainTransformResponse> commitRemoteCallback = new AsyncCallback<DomainTransformResponse>() {
            public void onFailure(Throwable caught) {
                // resolve here
                if (!suppressErrors) {
                    if (caught instanceof DomainTransformRequestException) {
                        final DomainTransformRequestException dtre = (DomainTransformRequestException) caught;
                        Callback<ClientTransformExceptionResolutionToken> callback = new Callback<ClientTransformExceptionResolutionToken>() {
                            public void apply(
                                    ClientTransformExceptionResolutionToken resolutionToken) {
                                if (resolutionToken
                                        .getResolverAction() == ClientTransformExceptionResolverAction.RESUBMIT) {
                                    eventIdsToIgnore = resolutionToken
                                            .getEventIdsToIgnore();
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
                        getTransformExceptionResolver().resolve(dtre, callback);
                        return;
                    }
                    if (ClientUtils.maybeOffline(caught)) {
                        fireStateChanged(OFFLINE);
                    }
                    throw new UnknownTransformFailedException(caught);
                }
                fireStateChanged(ERROR);
            }

            public void onSuccess(DomainTransformResponse response) {
                try {
                    LooseContext.pushWithTrue(
                            CommitToStorageTransformListener.CONTEXT_REPLAYING_SYNTHESISED_EVENTS);
                    onSuccess0(response);
                } finally {
                    LooseContext.pop();
                }
            }

            private void onSuccess0(DomainTransformResponse response) {
                PermissionsManager.get().setOnlineState(OnlineState.ONLINE);
                TransformManager tm = TransformManager.get();
                tm.setReplayingRemoteEvent(true);
                try {
                    synthesisedEvents = new ArrayList<DomainTransformEvent>();
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
                            idEvt = new DomainTransformEvent();
                            idEvt.setObjectClass(dte.getObjectClass());
                            idEvt.setObjectId(id);
                            idEvt.setPropertyName(
                                    TransformManager.LOCAL_ID_FIELD_NAME);
                            idEvt.setNewStringValue("0");
                            idEvt.setValueClass(Long.class);
                            idEvt.setTransformType(
                                    TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
                            synthesisedEvents.add(idEvt);
                            localToServerIds.put(dte.getObjectLocalId(), id);
                            tm.registerHiliMappingPriorToLocalIdDeletion(
                                    dte.getObjectClass(), id,
                                    dte.getObjectLocalId());
                        }
                        // if (dte.getObjectVersionNumber() != 0 && id != 0) {
                        // if we have zero id at this stage, we're probably in a
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
                            tm.consume(dte);
                            tm.fireDomainTransform(dte);// this notifies
                            // gears?
                            // well, definitely notifies clients who need to
                            // know chanages were committed
                        } catch (DomainTransformException e) {
                            // shouldn't happen, if the server code's ok
                            // note - squelching (for the moment)
                            // sourceentitynotfound -
                            // assume object was either deregistered or deleted
                            // before these transforms
                            // made it back from the server
                            if (e.getType() == DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND
                                    || e.getType() == DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND) {
                            } else {
                                throw new WrappedRuntimeException(e);
                            }
                        }
                    }
                    List<DomainTransformEvent> items = dtr.getEvents();
                    for (DomainTransformEvent evt : items) {
                        TransformManager.get().setTransformCommitType(evt,
                                CommitType.ALL_COMMITTED);
                    }
                    for (int i = priorRequestsWithoutResponse.size()
                            - 1; i >= 0; i--) {
                        if (priorRequestsWithoutResponse.get(i)
                                .getRequestId() <= dtr.getRequestId()) {
                            priorRequestsWithoutResponse.remove(i);
                        }
                    }
                    if (response.getMessage() != null) {
                        Registry.impl(ClientNotifications.class)
                                .showMessage(response.getMessage());
                    }
                } finally {
                    tm.setReplayingRemoteEvent(false);
                    if (reloadRequired) {
                        fireStateChanged(RELOAD);
                    } else {
                        fireStateChanged(COMMITTED);
                    }
                }
            }
        };
        // the ordering here is tricky
        // 'committing' says 'we have a flat list of rqs without response (incl
        // current)
        // wheras the transform rpc call requires rq (current) with prior rqs as
        // a listfield
        // given listener callbacks can be multi-threaded (jvm version),
        // use the following ordering - note we use a new list in
        // dtr.setPriorRequestsWithoutResponse(priorRequestsWithoutResponseCopy)
        List<DomainTransformRequest> priorRequestsWithoutResponseForCommit = new ArrayList<DomainTransformRequest>(
                priorRequestsWithoutResponse);
        dtr.setPriorRequestsWithoutResponse(
                priorRequestsWithoutResponseForCommit);
        committingRequest = dtr;
        fireStateChanged(COMMITTING);
        priorRequestsWithoutResponse.add(dtr);
        if (isLocalStorageOnly()) {
            fireStateChanged(OFFLINE);
            return;
        }
        if (PermissionsManager.get().getOnlineState() == OnlineState.OFFLINE) {
            ClientBase.getCommonRemoteServiceAsyncInstance()
                    .ping(new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            // ignore - expected(ish) behaviour - if it's a
                            // non-'offline' error, it's more graceful to ignore
                            // here than not
                            fireStateChanged(OFFLINE);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            if (canTransitionToOnline()) {
                                commitRemote(commitRemoteCallback);
                            }
                        }
                    });
        } else {
            commitRemote(commitRemoteCallback);
        }
    }

    protected void commitRemote(
            AsyncCallback<DomainTransformResponse> callback) {
        ClientBase.getCommonRemoteServiceAsyncInstance()
                .transform(committingRequest, callback);
        committingRequest = null;
    }

    @Override
    protected void fireStateChanged(String newState) {
        currentState = newState;
        super.fireStateChanged(newState);
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

    public static class UnknownTransformFailedException
            extends WrappedRuntimeException {
        public UnknownTransformFailedException(Throwable cause) {
            super(cause);
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

    class OneoffListenerWrapper implements StateChangeListener {
        private final AsyncCallback callback;

        public OneoffListenerWrapper(AsyncCallback callback) {
            this.callback = callback;
        }

        @Override
        public void stateChanged(Object source, String newState) {
            if (newState.equals(COMMITTING)) {
            } else if (newState.equals(COMMITTED) || newState.equals(OFFLINE)) {
                removeStateChangeListener(OneoffListenerWrapper.this);
                if (!reloadRequired) {
                    callback.onSuccess(null);
                }
            } else {
                removeStateChangeListener(OneoffListenerWrapper.this);
                if (!reloadRequired) {
                    callback.onFailure(new Exception("flush failed on server"));
                }
            }
        }
    }

    public static void flushAndRun(Runnable runnable) {
        Registry.impl(CommitToStorageTransformListener.class)
                .flushWithOneoffCallback(new AsyncCallbackStd() {
                    @Override
                    public void onSuccess(Object result) {
                        runnable.run();
                    }
                });
    }
}