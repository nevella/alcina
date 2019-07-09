package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.event.shared.UmbrellaException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestTagProvider;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.RemoteTransformPersister;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.logic.EntityLayerTransformPropogation;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.servlet.CascadingTransformSupport;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.actionhandlers.DtrSimpleAdminPersistenceHandler;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet.IsWrappedObjectDteFilter;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = ServletLayerTransforms.class, implementationType = ImplementationType.SINGLETON)
public class ServletLayerTransforms {
    private static final String TOPIC_UNEXPECTED_TRANSFORM_PERSISTENCE_EXCEPTION = CommonRemoteServiceServlet.class
            .getName() + ".TOPIC_UNEXPECTED_TRANSFORM_PERSISTENCE_EXCEPTION";

    public static final transient String CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH = ServletLayerUtils.class
            .getName() + ".CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH";

    public static final transient String CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK = ServletLayerUtils.class
            .getName() + ".CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK";

    public static void commitLocalTransformsInChunks(
            int maxTransformChunkSize) {
        try {
            ThreadedPermissionsManager.cast().callWithPushedSystemUserIfNeeded(
                    () -> commitLocalTranformInChunks0(maxTransformChunkSize));
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        } finally {
            ThreadlocalTransformManager.cast().resetTltm(null);
        }
    }

    public static ServletLayerTransforms get() {
        return Registry.impl(ServletLayerTransforms.class);
    }

    public static DomainTransformLayerWrapper pushTransforms(String tag,
            boolean asRoot, boolean returnResponse) {
        if (tag == null) {
            tag = DomainTransformRequestTagProvider.get().getTag();
        }
        int pendingTransformCount = TransformManager.get()
                .getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
        if (pendingTransformCount == 0) {
            ThreadlocalTransformManager.cast().resetTltm(null);
            return new DomainTransformLayerWrapper();
        }
        if (AppPersistenceBase.isTest() && !ResourceUtilities
                .is(ServletLayerUtils.class, "testTransformCascade")) {
            return new DomainTransformLayerWrapper();
        }
        int maxTransformChunkSize = ResourceUtilities.getInteger(
                ServletLayerUtils.class, "maxTransformChunkSize", 10000);
        if (pendingTransformCount > maxTransformChunkSize && !LooseContext
                .is(ServletLayerTransforms.CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK)) {
            commitLocalTransformsInChunks(maxTransformChunkSize);
            return new DomainTransformLayerWrapper();
        }
        return get().doPersistTransforms(tag, asRoot);
    }

    public static long pushTransformsAndGetFirstCreationId(boolean asRoot) {
        DomainTransformResponse transformResponse = pushTransforms(null, asRoot,
                true).response;
        DomainTransformEvent first = CommonUtils
                .first(transformResponse.getEventsToUseForClientUpdate());
        return first == null ? 0 : first.getGeneratedServerId();
    }

    public static long pushTransformsAndReturnId(boolean asRoot,
            HasIdAndLocalId returnIdFor) {
        DomainTransformResponse transformResponse = pushTransforms(null, asRoot,
                true).response;
        for (DomainTransformEvent dte : transformResponse
                .getEventsToUseForClientUpdate()) {
            if (dte.getObjectLocalId() == returnIdFor.getLocalId()
                    && dte.getObjectClass() == returnIdFor.getClass()
                    && dte.getTransformType() == TransformType.CREATE_OBJECT) {
                return dte.getGeneratedServerId();
            }
        }
        throw new RuntimeException(
                "Generated object not found - " + returnIdFor);
    }

    public static int pushTransformsAsCurrentUser() {
        return pushTransforms(false);
    }

    public static int pushTransformsAsRoot() {
        return pushTransforms(true);
    }

    public static TopicSupport<TransformPersistenceToken> topicUnexpectedExceptionBeforePostTransform() {
        return new TopicSupport<>(
                TOPIC_UNEXPECTED_TRANSFORM_PERSISTENCE_EXCEPTION);
    }

    private static Object commitLocalTranformInChunks0(
            int maxTransformChunkSize) throws Exception {
        String ipAddress = null;
        HttpServletRequest contextThreadLocalRequest = CommonRemoteServiceServlet
                .getContextThreadLocalRequest();
        long extClientInstanceId = 0;
        if (contextThreadLocalRequest != null) {
            ipAddress = ServletLayerUtils
                    .robustGetRemoteAddr(contextThreadLocalRequest);
            extClientInstanceId = PermissionsManager.get().getClientInstance()
                    .getId();
        }
        final ClientInstance commitInstance = Registry
                .impl(CommonPersistenceProvider.class).getCommonPersistence()
                .createClientInstance(Ax.format(
                        "servlet-bulk: %s - derived from client instance : %s",
                        EntityLayerUtils.getLocalHostName(),
                        extClientInstanceId), null, ipAddress);
        List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>(
                TransformManager.get()
                        .getTransformsByCommitType(CommitType.TO_LOCAL_BEAN));
        TransformManager.get()
                .getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).clear();
        ThreadlocalTransformManager.cast().resetTltm(null);
        DomainTransformRequest request = DomainTransformRequest
                .createPersistableRequest();
        request.setProtocolVersion(
                new DTESerializationPolicy().getTransformPersistenceProtocol());
        request.setRequestId(1);
        request.setClientInstance(commitInstance);
        request.setEvents(transforms);
        DeltaApplicationRecord dar = new DeltaApplicationRecord(request,
                DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED, false);
        DtrSimpleAdminPersistenceHandler persistenceHandler = new DtrSimpleAdminPersistenceHandler();
        persistenceHandler.commit(dar, maxTransformChunkSize);
        Exception ex = persistenceHandler.getJobTracker().getJobException();
        if (ex != null) {
            throw ex;
        }
        return null;
    }

    private static int pushTransforms(boolean asRoot) {
        int pendingTransformCount = TransformManager.get()
                .getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
        if (AppPersistenceBase.isTest() && !ResourceUtilities
                .is(ServletLayerUtils.class, "testTransformCascade")) {
            if (!LooseContext.is(
                    ServletLayerTransforms.CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH)) {
                TransformManager.get().clearTransforms();
            }
            return pendingTransformCount;
        }
        pushTransforms(null, asRoot, true);
        return pendingTransformCount;
    }

    private BackendTransformQueue backendTransformQueue;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * the instance used by the server layer when acting as a client to the ejb
     * layer. Note - this must be set on webapp startup
     */
    private ClientInstance serverAsClientInstance;

    private Map<Long, HiliLocatorMap> clientInstanceLocatorMap = new HashMap<Long, HiliLocatorMap>();

    private int transformRequestCounter = 1;

    public void appShutdown() {
        backendTransformQueue.appShutdown();
    }

    public synchronized void enqueueBackendTransform(Runnable runnable) {
        if (backendTransformQueue == null) {
            backendTransformQueue = new BackendTransformQueue();
            backendTransformQueue.start();
        }
        backendTransformQueue.enqueue(runnable);
    }

    public HiliLocatorMap getLocatorMapForClient(
            ClientInstance clientInstance) {
        Long clientInstanceId = clientInstance.getId();
        Map<Long, HiliLocatorMap> clientInstanceLocatorMap = getClientInstanceLocatorMap();
        synchronized (clientInstanceLocatorMap) {
            if (!clientInstanceLocatorMap.containsKey(clientInstanceId)) {
                HiliLocatorMap locatorMap = CommonPersistenceProvider.get()
                        .getCommonPersistenceExTransaction()
                        .getLocatorMap(clientInstanceId);
                clientInstanceLocatorMap.put(clientInstanceId, locatorMap);
            }
        }
        HiliLocatorMap locatorMap = clientInstanceLocatorMap
                .get(clientInstanceId);
        return locatorMap;
    }

    public HiliLocatorMap getLocatorMapForClient(
            DomainTransformRequest request) {
        return getLocatorMapForClient(request.getClientInstance());
    }

    public ClientInstance getServerAsClientInstance() {
        return this.serverAsClientInstance;
    }

    public void setServerAsClientInstance(
            ClientInstance serverAsClientInstance) {
        this.serverAsClientInstance = serverAsClientInstance;
    }

    public DomainTransformLayerWrapper transformFromServletLayer(
            Collection<DomainTransformEvent> transforms, String tag)
            throws DomainTransformRequestException {
        DomainTransformRequest request = new DomainTransformRequest();
        HiliLocatorMap map = new HiliLocatorMap();
        request.setClientInstance(Registry.impl(ServletLayerTransforms.class)
                .getServerAsClientInstance());
        request.setTag(tag);
        request.setRequestId(nextTransformRequestId());
        for (DomainTransformEvent dte : transforms) {
            dte.setCommitType(CommitType.TO_STORAGE);
        }
        request.getEvents().addAll(transforms);
        try {
            ThreadedPermissionsManager.cast().pushSystemUser();
            TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
                    request, map, Registry.impl(TransformLoggingPolicy.class),
                    false, false, false, logger, true);
            persistenceToken.setOriginatingUserId(LooseContext
                    .get(CommonRemoteServiceServlet.CONTEXT_RPC_USER_ID));
            return submitAndHandleTransforms(persistenceToken);
        } finally {
            ThreadedPermissionsManager.cast().popSystemUser();
        }
    }

    public DomainTransformLayerWrapper transformFromServletLayer(String tag)
            throws DomainTransformRequestException {
        LinkedHashSet<DomainTransformEvent> pendingTransforms = TransformManager
                .get().getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
        if (pendingTransforms.isEmpty()) {
            return null;
        }
        ArrayList<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>(
                pendingTransforms);
        pendingTransforms.clear();
        return transformFromServletLayer(items, tag);
    }

    private void logTransformException(DomainTransformResponse response) {
        logger.warn(String.format(
                "domain transform problem - clientInstance: %s - rqId: %s - user ",
                response.getRequest().getClientInstance().getId(),
                response.getRequestId(),
                PermissionsManager.get().getUserName()));
        List<DomainTransformException> transformExceptions = response
                .getTransformExceptions();
        for (DomainTransformException ex : transformExceptions) {
            logger.warn("Per-event error: " + ex.getMessage());
            if (ex.getEvent() != null) {
                logger.warn("Event: " + ex.getEvent().toDebugString());
            }
        }
        File file = DataFolderProvider.get()
                .getChildFile(Ax.format("dtr-exception/%s.txt", LocalDateTime
                        .now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        file.getParentFile().mkdirs();
        ResourceUtilities.write(response.getRequest().toString(), file);
        logger.warn(
                Ax.format("Request with exceptions written to: \n\t%s", file));
    }

    protected DomainTransformLayerWrapper doPersistTransforms(String tag,
            boolean asRoot) {
        // for debugging
        Set<DomainTransformEvent> transforms = TransformManager.get()
                .getTransforms();
        ThreadedPermissionsManager tpm = ThreadedPermissionsManager.cast();
        boolean muted = MetricLogging.get().isMuted();
        try {
            MetricLogging.get().setMuted(true);
            if (asRoot) {
                tpm.pushSystemUser();
            } else {
                tpm.pushCurrentUser();
            }
            CascadingTransformSupport cascadingTransformSupport = CascadingTransformSupport
                    .get();
            try {
                cascadingTransformSupport.beforeTransform();
                DomainTransformLayerWrapper wrapper = get()
                        .transformFromServletLayer(tag);
                // see preamble to cascading transform support
                while (cascadingTransformSupport.hasChildren()) {
                    logger.debug(
                            "Servlet layer - waiting for cascading transforms");
                    synchronized (cascadingTransformSupport) {
                        if (cascadingTransformSupport.hasChildren()) {
                            cascadingTransformSupport.wait();
                        }
                    }
                }
                UmbrellaException childException = cascadingTransformSupport
                        .getException();
                if (childException != null) {
                    throw childException;
                }
                return wrapper;
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            } finally {
                cascadingTransformSupport.afterTransform();
            }
        } finally {
            tpm.popUser();
            ThreadlocalTransformManager.cast().resetTltm(null);
            MetricLogging.get().setMuted(muted);
        }
    }

    protected void handleWrapperTransforms() {
        EntityLayerTransformPropogation transformPropogation = Registry
                .impl(EntityLayerTransformPropogation.class, void.class, true);
        if (transformPropogation == null) {
            return;
        }
        ThreadlocalTransformManager.cast().getTransforms();
        LinkedHashSet<DomainTransformEvent> pendingTransforms = TransformManager
                .get().getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
        if (pendingTransforms.isEmpty()) {
            return;
        }
        final List<DomainTransformEvent> items = CollectionFilters
                .filter(pendingTransforms, new IsWrappedObjectDteFilter());
        pendingTransforms.removeAll(items);
        if (!items.isEmpty() && !pendingTransforms.isEmpty()) {
            throw new RuntimeException("Non-wrapped and wrapped object"
                    + " transforms registered after transformPerist()");
        }
        if (items.isEmpty()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    int depth = LooseContext.depth();
                    transformFromServletLayer(items, null);
                    LooseContext.confirmDepth(depth);
                    ThreadlocalTransformManager.cast().resetTltm(null);
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            };
        }.start();
    }

    protected DomainTransformLayerWrapper submitAndHandleTransforms(
            TransformPersistenceToken persistenceToken)
            throws DomainTransformRequestException {
        List<TransformPersistenceToken> perStoreTokens = persistenceToken
                .toPerStoreTokens();
        if (perStoreTokens.size() == 1) {
            TransformPersistenceToken perStoreToken = perStoreTokens.get(0);
            if (perStoreToken.provideTargetsWritableStore()) {
                return submitAndHandleTransformsWritableStore(perStoreToken);
            }
        }
        DomainTransformLayerWrapper result = new DomainTransformLayerWrapper();
        for (TransformPersistenceToken perStoreToken : perStoreTokens) {
            if (perStoreToken.provideTargetsWritableStore()) {
                result.merge(
                        submitAndHandleTransformsWritableStore(perStoreToken));
            } else {
                DomainTransformLayerWrapper remoteWrapperResult = Registry
                        .impl(RemoteTransformPersister.class,
                                perStoreToken.getTargetStore()
                                        .getDomainDescriptor().getClass())
                        .submitAndHandleTransformsRemoteStore(perStoreToken);
                result.merge(remoteWrapperResult);
            }
        }
        return result;
    }

    protected DomainTransformLayerWrapper submitAndHandleTransformsWritableStore(
            TransformPersistenceToken persistenceToken)
            throws DomainTransformRequestException {
        boolean unexpectedException = true;
        try {
            LooseContext.getContext().push();
            AppPersistenceBase.checkNotReadOnly();
            DomainStore.stores().writableStore().getPersistenceEvents()
                    .fireDomainTransformPersistenceEvent(
                            new DomainTransformPersistenceEvent(
                                    persistenceToken, null, true));
            MetricLogging.get().start("transform-commit");
            DomainTransformLayerWrapper wrapper = Registry
                    .impl(TransformPersistenceQueue.class)
                    .submit(persistenceToken);
            MetricLogging.get().end("transform-commit");
            handleWrapperTransforms();
            wrapper.ignored = persistenceToken.ignored;
            DomainStore.stores().writableStore().getPersistenceEvents()
                    .fireDomainTransformPersistenceEvent(
                            new DomainTransformPersistenceEvent(
                                    persistenceToken, wrapper, true));
            unexpectedException = false;
            if (wrapper.response
                    .getResult() == DomainTransformResponseResult.OK) {
                wrapper.response.setLogOffset(wrapper.getLogOffset());
                return wrapper;
            } else {
                logTransformException(wrapper.response);
                throw new DomainTransformRequestException(wrapper.response);
            }
        } finally {
            if (unexpectedException) {
                try {
                    topicUnexpectedExceptionBeforePostTransform()
                            .publish(persistenceToken);
                } catch (Throwable t) {
                    // make sure we get out alive
                    t.printStackTrace();
                }
            }
            LooseContext.getContext().pop();
        }
    }

    /**
     * synchronizing implies serialized transforms per clientInstance
     */
    protected DomainTransformLayerWrapper transform(
            DomainTransformRequest request, boolean ignoreClientAuthMismatch,
            boolean forOfflineTransforms,
            boolean blockUntilAllListenersNotified)
            throws DomainTransformRequestException {
        HiliLocatorMap locatorMap = Registry.impl(ServletLayerTransforms.class)
                .getLocatorMapForClient(request);
        synchronized (locatorMap) {
            TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
                    request, locatorMap,
                    Registry.impl(TransformLoggingPolicy.class), true,
                    ignoreClientAuthMismatch, forOfflineTransforms, logger,
                    blockUntilAllListenersNotified);
            return submitAndHandleTransforms(persistenceToken);
        }
    }

    Map<Long, HiliLocatorMap> getClientInstanceLocatorMap() {
        return this.clientInstanceLocatorMap;
    }

    int getTransformRequestCounter() {
        return this.transformRequestCounter;
    }

    synchronized int nextTransformRequestId() {
        return transformRequestCounter++;
    }
}
