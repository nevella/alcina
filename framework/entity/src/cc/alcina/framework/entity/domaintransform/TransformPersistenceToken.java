package cc.alcina.framework.entity.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicyFactory;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class TransformPersistenceToken implements Serializable {
    private final DomainTransformRequest request;

    private HiliLocatorMap locatorMap;

    private int dontFlushTilNthTransform = 0;

    public int ignored = 0;

    private final boolean asyncClient;

    private Pass pass = Pass.TRY_COMMIT;

    private Set<DomainTransformEvent> ignoreInExceptionPass = new LinkedHashSet<DomainTransformEvent>();

    private PersistenceLayerTransformExceptionPolicy transformExceptionPolicy;

    private List<DomainTransformException> transformExceptions = new ArrayList<DomainTransformException>();

    private List<DomainTransformEvent> clientUpdateEvents = new ArrayList<DomainTransformEvent>();

    private boolean ignoreClientAuthMismatch;

    private boolean forOfflineTransforms;

    private transient Logger logger;

    private TransformLoggingPolicy transformLoggingPolicy;

    private boolean blockUntilAllListenersNotified;

    private Long originatingUserId;

    private transient DomainStore targetStore = null;

    public TransformPersistenceToken(DomainTransformRequest request,
            HiliLocatorMap locatorMap,
            TransformLoggingPolicy transformLoggingPolicy, boolean asyncClient,
            boolean ignoreClientAuthMismatch, boolean forOfflineTransforms,
            Logger logger, boolean blockUntilAllListenersNotified) {
        this.request = request;
        this.locatorMap = locatorMap;
        this.transformLoggingPolicy = transformLoggingPolicy;
        this.asyncClient = asyncClient;
        this.ignoreClientAuthMismatch = ignoreClientAuthMismatch;
        this.forOfflineTransforms = forOfflineTransforms;
        this.logger = logger;
        this.blockUntilAllListenersNotified = blockUntilAllListenersNotified;
        this.transformExceptionPolicy = Registry
                .impl(PersistenceLayerTransformExceptionPolicyFactory.class)
                .getPolicy(this, forOfflineTransforms);
    }

    public List<DomainTransformEvent> getClientUpdateEvents() {
        return this.clientUpdateEvents;
    }

    public int getDontFlushTilNthTransform() {
        return dontFlushTilNthTransform;
    }

    public Set<DomainTransformEvent> getIgnoreInExceptionPass() {
        return this.ignoreInExceptionPass;
    }

    public HiliLocatorMap getLocatorMap() {
        return this.locatorMap;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Long getOriginatingUserId() {
        return this.originatingUserId;
    }

    public Pass getPass() {
        return pass;
    }

    public DomainTransformRequest getRequest() {
        return this.request;
    }

    public DomainStore getTargetStore() {
        return this.targetStore;
    }

    public PersistenceLayerTransformExceptionPolicy getTransformExceptionPolicy() {
        return transformExceptionPolicy;
    }

    public List<DomainTransformException> getTransformExceptions() {
        return this.transformExceptions;
    }

    public TransformLoggingPolicy getTransformLoggingPolicy() {
        return this.transformLoggingPolicy;
    }

    public boolean isAsyncClient() {
        return this.asyncClient;
    }

    public boolean isBlockUntilAllListenersNotified() {
        return this.blockUntilAllListenersNotified;
    }

    public boolean isForOfflineTransforms() {
        return this.forOfflineTransforms;
    }

    public boolean isIgnoreClientAuthMismatch() {
        return ignoreClientAuthMismatch;
    }

    public boolean provideTargetsWritableStore() {
        return targetStore == DomainStore.stores().writableStore();
    }

    public void setBlockUntilAllListenersNotified(
            boolean blockUntilAllListenersNotified) {
        this.blockUntilAllListenersNotified = blockUntilAllListenersNotified;
    }

    public void setClientUpdateEvents(
            List<DomainTransformEvent> clientUpdateEvents) {
        this.clientUpdateEvents = clientUpdateEvents;
    }

    public void setDontFlushTilNthTransform(int dontFlushTilNthTransform) {
        this.dontFlushTilNthTransform = dontFlushTilNthTransform;
    }

    public void setForOfflineTransforms(boolean forClientTransforms) {
        this.forOfflineTransforms = forClientTransforms;
    }

    public void setIgnoreClientAuthMismatch(boolean ignoreClientAuthMismatch) {
        this.ignoreClientAuthMismatch = ignoreClientAuthMismatch;
    }

    public void setLocatorMap(HiliLocatorMap locatorMap) {
        this.locatorMap = locatorMap;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setOriginatingUserId(Long originatingUserId) {
        this.originatingUserId = originatingUserId;
    }

    public void setPass(Pass pass) {
        this.pass = pass;
    }

    public void setTransformExceptionPolicy(
            PersistenceLayerTransformExceptionPolicy transformExceptionPolicy) {
        this.transformExceptionPolicy = transformExceptionPolicy;
    }

    public void setTransformLoggingPolicy(
            TransformLoggingPolicy transformLoggingPolicy) {
        this.transformLoggingPolicy = transformLoggingPolicy;
    }

    public List<TransformPersistenceToken> toPerStoreTokens() {
        // at the moment, we just implement for exactly one store - and assume
        // (if not writable store) that the 'client' is the servlet layer app
        // CachingMap<DomainStore, TransformPersistenceToken> map = new
        // CachingMap<>(
        // store -> {
        // DomainTransformRequest request = new DomainTransformRequest();
        // TransformPersistenceToken token = new TransformPersistenceToken(
        // request, locatorMap, transformLoggingPolicy,
        // asyncClient, ignoreClientAuthMismatch,
        // forOfflineTransforms, logger,
        // blockUntilAllListenersNotified);
        // return token;
        // });
        // request.getPriorRequestsWithoutResponse();
        Set<DomainStore> targetStores = request.allTransforms().stream()
                .map(DomainTransformEvent::getObjectClass)
                .map(DomainStore.stores()::storeFor)
                .map(store -> Ax.nullTo(store, DomainStore.writableStore()))
                .collect(Collectors.toSet());
        Preconditions.checkState(targetStores.size() == 1);
        targetStore = targetStores.stream().findFirst().get();
        return Collections.singletonList(this);
    }

    public enum Pass {
        TRY_COMMIT, DETERMINE_EXCEPTION_DETAIL, RETRY_WITH_IGNORES, FAIL
    }
}
