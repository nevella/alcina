package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientUIThreadWorker;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaSignature;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DeltaApplicationRecordSerializerImpl;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DomainTrancheProtocolHandler;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.renderer.ToStringConverter;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.util.AsyncCallbackNull;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.Lzw;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

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
            .getName() + "."
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

    private Map<Integer, DeltaApplicationRecord> persistedTransforms = new HashMap<Integer, DeltaApplicationRecord>();

    private boolean closing = false;

    private Long clientInstanceIdForGet = null;

    private static LocalTransformPersistence localTransformPersistence;

    private ClientInstance domainObjectsPersistedBy;

    private boolean useLzw;

    public LocalTransformPersistence() {
    }

    public abstract void clearPersistedClient(ClientInstance exceptFor,
            int exceptForId, AsyncCallback callback, boolean clearDeltaStore);

    public void dumpDatabase(final Callback<String> callback) {
        AsyncCallback<Iterator<DeltaApplicationRecord>> transformCallback = new AsyncCallbackStd<Iterator<DeltaApplicationRecord>>() {
            @Override
            public void onSuccess(Iterator<DeltaApplicationRecord> result) {
                StringBuilder sb = new StringBuilder();
                CommonUtils.iteratorToList(result).forEach(wrapper -> {
                    sb.append(new DeltaApplicationRecordSerializerImpl()
                            .write(wrapper));
                    sb.append("\n");
                });
                callback.apply(sb.toString());
            }
        };
        getTransforms(new DeltaApplicationRecordType[0], transformCallback);
    }

    public CommitToStorageTransformListener getCommitToStorageTransformListener() {
        return commitToStorageTransformListener;
    }

    public Map<Integer, DeltaApplicationRecord> getPersistedTransforms() {
        return this.persistedTransforms;
    }

    public abstract String getPersistenceStoreName();

    public DTESerializationPolicy getSerializationPolicy() {
        return serializationPolicy;
    }

    public void handleUncommittedTransformsOnLoad(
            final AsyncCallback<Void> cb) {
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

    public void persistableTransform(DomainTransformRequest dtr,
            DeltaApplicationRecordType type) {
        dtr.setProtocolVersion(
                getSerializationPolicy().getTransformPersistenceProtocol());
        if (!dtr.getEvents().isEmpty()) {
            if (!closing) {
                new DTRAsyncSerializer(dtr, type).start();
            } else {
                DeltaApplicationRecord wrapper = new DeltaApplicationRecord(dtr,
                        type, false);
                persist(wrapper, new AsyncCallbackNull());
            }
        }
    }

    public void recordDeltaApplication(DomainModelDeltaSignature signature,
            AsyncCallback<Void> AsyncCallback) {
        ClientInstance clientInstance = PermissionsManager.get()
                .getClientInstance();
        DeltaApplicationRecord wrapper = new DeltaApplicationRecord(0,
                signature.toString(), System.currentTimeMillis(),
                PermissionsManager.get().getUserId(), clientInstance.getId(), 0,
                clientInstance.getAuth(),
                DeltaApplicationRecordType.REMOTE_DELTA_APPLIED,
                DomainTrancheProtocolHandler.VERSION, "");
        persist(wrapper, AsyncCallback);
    }

    public abstract void reparentToClientInstance(
            DeltaApplicationRecord wrapper, ClientInstance clientInstance,
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
                final DeltaApplicationRecord wrapper = new DeltaApplicationRecord(
                        rq, DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED,
                        false);
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
            final Set<Integer> removeIds = new HashSet(
                    getPersistedTransforms().keySet());
            for (DomainTransformRequest rq : rqs) {
                removeIds.remove(rq.getRequestId());
            }
            List<DeltaApplicationRecord> persistedWrappers = new ArrayList<DeltaApplicationRecord>();
            for (Integer i : removeIds) {
                DeltaApplicationRecord wrapper = getPersistedTransforms()
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
                    ClientInstance clientInstance = PermissionsManager.get()
                            .getClientInstance();
                    rq.setClientInstance(clientInstance);
                    rq.setRequestId(0);
                    rq.setEvents(new ArrayList<DomainTransformEvent>(
                            getCommitToStorageTransformListener()
                                    .getSynthesisedEvents()));
                    rq.setProtocolVersion(getSerializationPolicy()
                            .getTransformPersistenceProtocol());
                    DeltaApplicationRecord wrapper = new DeltaApplicationRecord(
                            rq, DeltaApplicationRecordType.REMOTE_DELTA_APPLIED,
                            false);
                    persist(wrapper, new AsyncCallbackNull());
                }
            };
            transformPersisted(persistedWrappers,
                    afterTransformsMarkedAsPersistedCallback);
            return;
        } else if (newState == CommitToStorageTransformListener.RELOAD) {
            clearAllPersisted(new AsyncCallbackNull());
        }
    }

    protected abstract void clearAllPersisted(AsyncCallback callback);

    protected Long getClientInstanceIdForGet() {
        return clientInstanceIdForGet;
    }

    protected void getTransforms(DeltaApplicationRecordType type,
            AsyncCallback<Iterator<DeltaApplicationRecord>> callback) {
        getTransforms(new DeltaApplicationRecordType[] { type }, callback);
    }

    protected void getTransforms(final DeltaApplicationRecordType[] types,
            final AsyncCallback<Iterator<DeltaApplicationRecord>> callback) {
        DeltaApplicationFilters filters = new DeltaApplicationFilters();
        filters.types = types;
        getTransforms(filters, callback);
    }

    protected abstract void getTransforms(DeltaApplicationFilters filters,
            AsyncCallback<Iterator<DeltaApplicationRecord>> callback);

    private static final String LZW_PROTOCOL_ADDITION = "/lzw";

    protected void maybeCompressWrapper(DeltaApplicationRecord wrapper) {
        if (isUseLzw() && wrapper.getProtocolVersion() != null && !wrapper
                .getProtocolVersion().endsWith(LZW_PROTOCOL_ADDITION)) {
            wrapper.setText(new Lzw().compress(wrapper.getText()));
            wrapper.setProtocolVersion(
                    wrapper.getProtocolVersion() + LZW_PROTOCOL_ADDITION);
        }
    }

    protected void maybeDecompressWrapper(DeltaApplicationRecord wrapper) {
        if (wrapper.getProtocolVersion().endsWith(LZW_PROTOCOL_ADDITION)) {
            wrapper.setText(new Lzw().decompress(wrapper.getText()));
        }
    }

    private LinkedHashMap<DeltaApplicationRecord, AsyncCallback> recordQueue = new LinkedHashMap<DeltaApplicationRecord, AsyncCallback>();

    class ProcessRecordQueueCallback implements AsyncCallback {
        private AsyncCallback successCallback;

        private AsyncCallback failureCallback;

        ProcessRecordQueueCallback(AsyncCallback successCallback,
                AsyncCallback failureCallback) {
            this.successCallback = successCallback;
            this.failureCallback = failureCallback;
        }

        @Override
        public void onFailure(Throwable caught) {
            failureCallback.onFailure(caught);
        }

        @Override
        public void onSuccess(Object result) {
            if (successCallback != null) {
                successCallback.onSuccess(result);
            }
            persistQueue();
        }
    }

    public void persist(List<DeltaApplicationRecord> wrappers,
            AsyncCallback callback) {
        for (int i = 0; i < wrappers.size(); i++) {
            recordQueue.put(wrappers.get(i), new ProcessRecordQueueCallback(
                    i == wrappers.size() - 1 ? callback : null, callback));
        }
        persistQueue();
    }

    protected void persist(DeltaApplicationRecord wrapper,
            AsyncCallback callback) {
        recordQueue.put(wrapper,
                new ProcessRecordQueueCallback(callback, callback));
        persistQueue();
    }

    private void persistQueue() {
        if (!recordQueue.isEmpty()) {
            Entry<DeltaApplicationRecord, AsyncCallback> first = recordQueue
                    .entrySet().iterator().next();
            // copy in case removal zaps the refs
            DeltaApplicationRecord record = first.getKey();
            AsyncCallback callback = first.getValue();
            recordQueue.remove(record);
            persistFromFrontOfQueue(record, callback);
        }
    }

    protected abstract void persistFromFrontOfQueue(
            DeltaApplicationRecord wrapper, AsyncCallback callback);

    protected void persistOfflineTransforms(
            List<DeltaApplicationRecord> uncommitted, ModalNotifier notifier,
            AsyncCallback<Void> postPersistOfflineTransformsCallback) {
        ClientBase.getCommonRemoteServiceAsyncInstance()
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
        Registry.impl(ClientNotifications.class).showError(
                "Unable to open offline session",
                new Exception("Only one tab may be open "
                        + "for this application when opening offline. "));
    }

    protected void showUnableToLoadOfflineMessage() {
        Registry.impl(ClientNotifications.class)
                .showMessage("<b>Unable to open offline session</b><br><br>"
                        + "No data saved");
    }

    protected abstract void transformPersisted(
            List<DeltaApplicationRecord> persistedWrappers,
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

        Iterator<DeltaApplicationRecord> loadIterator;

        public Loader(LocalTransformPersistence localTransformPersistence,
                String data, Callback callback) {
            this.localTransformPersistence = localTransformPersistence;
            this.data = data;
            this.callback = callback;
        }

        public void start() {
            List<DeltaApplicationRecord> wrappers = new DeltaApplicationRecordSerializerImpl()
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
        DeltaApplicationRecord wrapper;

        StringBuffer sb = new StringBuffer();

        private List<DomainTransformEvent> items;

        DTRProtocolHandler handler;

        public DTRAsyncSerializer(DomainTransformRequest dtr,
                DeltaApplicationRecordType type) {
            super(1000, 200);
            wrapper = new DeltaApplicationRecord(dtr, type, true);
            items = dtr.getEvents();
            handler = new DTRProtocolSerializer().getHandler(
                    getSerializationPolicy().getTransformPersistenceProtocol());
        }

        @Override
        protected boolean isComplete() {
            return index == items.size();
        }

        protected void onComplete() {
            Registry.impl(ClientNotifications.class).metricLogStart("persist");
            sb = handler.finishSerialization(sb);
            wrapper.setText(sb.toString());
            persist(wrapper, new AsyncCallbackStd() {
                @Override
                public void onSuccess(Object result) {
                    Registry.impl(ClientNotifications.class)
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

    public static class DeltaApplicationFilters {
        public DeltaApplicationRecordType[] types = new DeltaApplicationRecordType[0];

        public Long clientInstanceId;

        public String protocolVersion;
    }

    public abstract void getDomainModelDeltaIterator(
            DeltaApplicationFilters filters,
            AsyncCallback<Iterator<DomainModelDelta>> callback);

    public static String stringListToClause(Collection<String> strs) {
        if (strs.isEmpty()) {
            return " ('') ";
        }
        StringBuffer sb = new StringBuffer();
        sb.append(" (");
        for (String str : strs) {
            sb.append(sb.length() == 2 ? "'" : ", '");
            sb.append(str.replace("'", "''"));
            sb.append("'");
        }
        sb.append(") ");
        return sb.toString();
    }

    public abstract void getClientInstanceIdOfDomainObjectDelta(
            AsyncCallback callback);

    protected String getTransformWrapperSql(DeltaApplicationFilters filters) {
        if (filters.clientInstanceId == null) {
            filters.clientInstanceId = getClientInstanceIdForGet();
        }
        List<String> clauses = new ArrayList<String>();
        if (filters.types.length > 0) {
            String typeClause = "transform_request_type in "
                    + stringListToClause(CollectionFilters.convert(
                            Arrays.asList(filters.types),
                            new ToStringConverter()));
            clauses.add(typeClause);
        }
        if (filters.clientInstanceId != null) {
            clauses.add(CommonUtils.formatJ("  clientInstance_id=%s ",
                    filters.clientInstanceId));
        }
        if (filters.protocolVersion != null) {
            clauses.add(CommonUtils.formatJ("  transform_event_protocol='%s' ",
                    filters.protocolVersion));
        }
        if (clauses.isEmpty()) {
            throw new RuntimeException("need some type of filter");
        }
        String sql = CommonUtils.formatJ(
                "select %s from TransformRequests"
                        + " where %s  order by id asc",
                getTransformWrapperSqlFields(),
                CommonUtils.join(clauses, " and "));
        return sql;
    }

    protected String getTransformWrapperSqlFields() {
        return "*";
    }

    protected String clearPersistedClientSql(ClientInstance exceptFor,
            int exceptForId) {
        final String sql = CommonUtils.formatJ("DELETE from TransformRequests"
                + " where (transform_request_type " +
                // legacy
                "in('CLIENT_OBJECT_LOAD','CLIENT_SYNC','TO_REMOTE_COMPLETED')"
                + " OR transform_request_type='%s'"
                + " OR transform_request_type='%s')"
                + " and (clientInstance_id != %s and id != %s)",
                DeltaApplicationRecordType.LOCAL_TRANSFORMS_REMOTE_PERSISTED,
                DeltaApplicationRecordType.REMOTE_DELTA_APPLIED,
                exceptFor == null ? -1 : exceptFor.getId(), exceptForId);
        return sql;
    }
}
