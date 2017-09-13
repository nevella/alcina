package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken.Pass;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy.TransformExceptionAction;
import cc.alcina.framework.entity.logic.EntityLayerObjects;

public class TransformPersister {
    private static final String TOPIC_PERSISTING_TRANSFORMS = TransformPersister.class
            .getName() + ".TOPIC_PERSISTING_TRANSFORMS";

    private EntityManager entityManager;

    private CommonPersistenceLocal commonPersistenceBase;

    public EntityManager getEntityManager() {
        return this.entityManager;
    }

    public DomainTransformLayerWrapper transformExPersistenceContext(
            TransformPersistenceToken token) {
        DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
        boolean perform = true;
        while (perform) {
            perform = false;
            try {
                LooseContext.pushWithTrue(
                        TransformManager.CONTEXT_DO_NOT_POPULATE_SOURCE);
                LooseContext.set(CONTEXT_TRANSFORM_LAYER_WRAPPER, wrapper);
                Registry.impl(CommonPersistenceProvider.class)
                        .getCommonPersistence()
                        .transformInPersistenceContext(this, token, wrapper);
            } catch (RuntimeException ex) {
                DeliberatelyThrownWrapperException dtwe = null;
                if (ex instanceof DeliberatelyThrownWrapperException) {
                    dtwe = (DeliberatelyThrownWrapperException) ex;
                } else if (ex
                        .getCause() instanceof DeliberatelyThrownWrapperException) {
                    dtwe = (DeliberatelyThrownWrapperException) ex.getCause();
                } else {
                    throw ex;
                }
            } finally {
                LooseContext.pop();
            }
            if (token.getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
                token.getRequest()
                        .updateTransformCommitType(CommitType.TO_STORAGE, true);
                DomainTransformException firstException = token
                        .getTransformExceptions().get(0);
                perform = !firstException.irresolvable();
            } else if (token.getPass() == Pass.RETRY_WITH_IGNORES) {
                token.setPass(Pass.TRY_COMMIT);
                perform = true;
            }
        }
        if (wrapper.response
                .getResult() == DomainTransformResponseResult.FAILURE) {
            Registry.impl(CommonPersistenceProvider.class)
                    .getCommonPersistence().expandExceptionInfo(wrapper);
        }
        return wrapper;
    }

    private void putExceptionInWrapper(TransformPersistenceToken token,
            Exception e, DomainTransformLayerWrapper wrapper) {
        if (e != null) {
            DomainTransformException transformException;
            if (e instanceof DomainTransformException) {
                transformException = (DomainTransformException) e;
            } else {
                transformException = new DomainTransformException(e);
                Registry.impl(JPAImplementation.class)
                        .interpretException(transformException);
            }
            if (!token.getTransformExceptions().contains(transformException)) {
                token.getTransformExceptions().add(transformException);
            }
        }
        DomainTransformResponse response = new DomainTransformResponse();
        response.setResult(DomainTransformResponseResult.FAILURE);
        DomainTransformRequest request = token.getRequest();
        response.setRequest(request);
        response.setRequestId(request.getRequestId());
        response.getTransformExceptions().clear();
        response.getTransformExceptions()
                .addAll(token.getTransformExceptions());
        wrapper.response = response;
    }

    private void possiblyAddSilentSkips(TransformPersistenceToken token,
            DomainTransformException transformException) {
        DomainTransformEvent event = transformException.getEvent();
        List<DomainTransformEvent> allTransforms = token.getRequest()
                .allTransforms();
        int i = allTransforms.indexOf(event) - 1;
        int addPos = token.getTransformExceptions().size() - 1;
        for (; i >= 0; i--) {
            DomainTransformEvent itrEvent = allTransforms.get(i);
            if (!event.related(itrEvent)) {
                break;
            } else {
                token.getIgnoreInExceptionPass().add(itrEvent);
                DomainTransformException silentSkip = new DomainTransformException(
                        itrEvent, transformException.getType());
                silentSkip.setSilent(true);
                token.getTransformExceptions().add(addPos, silentSkip);
            }
        }
    }

    private Integer getHighestPersistedRequestIdForClientInstance(
            long clientInstanceId) {
        String eql = String.format(
                "select max(dtrq.requestId) as maxId "
                        + "from %s dtrq where dtrq.clientInstance.id=%s ",
                commonPersistenceBase
                        .getImplementation(
                                DomainTransformRequestPersistent.class)
                        .getSimpleName(),
                clientInstanceId);
        Integer result = (Integer) getEntityManager().createQuery(eql)
                .getSingleResult();
        return result;
    }

    long determineExceptionDetailPassStartTime = 0;

    private int determinedExceptionCount;

    public static final String CONTEXT_REPLAYING_FOR_LOGS = ThreadlocalTransformManager.class
            .getName() + ".CONTEXT_REPLAYING_FOR_LOGS";

    public static final String CONTEXT_TRANSFORM_LAYER_WRAPPER = ThreadlocalTransformManager.class
            .getName() + ".CONTEXT_TRANSFORM_LAYER_WRAPPER";

    private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITH_DET_EXCEPTIONS = 20
            * 1000;

    private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITHOUT_EXCEPTIONS = 40
            * 1000;

    public void transformInPersistenceContext(
            final TransformPersistenceToken token,
            CommonPersistenceBase commonPersistenceBase,
            EntityManager entityManager, DomainTransformLayerWrapper wrapper) {
        this.entityManager = entityManager;
        this.commonPersistenceBase = commonPersistenceBase;
        IUser incomingUser = PermissionsManager.get().getUser();
        commonPersistenceBase.connectPermissionsManagerToLiveObjects(true);
        HiliLocatorMap locatorMap = token.getLocatorMap();
        HiliLocatorMap locatorMapClone = (HiliLocatorMap) locatorMap.clone();
        final DomainTransformRequest request = token.getRequest();
        List<DomainTransformEventPersistent> dtreps = wrapper.persistentEvents;
        List<DomainTransformRequestPersistent> dtrps = wrapper.persistentRequests;
        wrapper.locatorMap = locatorMap;
        try {
            ObjectPersistenceHelper.get();
            ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
            // We know this is thread-local, so we can clear the tm transforms
            // add the entity version checker now
            tm.resetTltm(locatorMap, token.getTransformExceptionPolicy(), true);
            tm.setEntityManager(getEntityManager());
            ClientInstance persistentClientInstance = (ClientInstance) commonPersistenceBase
                    .findImplInstance(ClientInstance.class,
                            request.getClientInstance().getId());
            if (persistentClientInstance == null
                    || (persistentClientInstance.getAuth() != null
                            && !(persistentClientInstance.getAuth().equals(
                                    request.getClientInstance().getAuth())))) {
                DomainTransformException ex = new DomainTransformException(
                        "Invalid client instance authentication");
                ex.setType(DomainTransformExceptionType.INVALID_AUTHENTICATION);
                putExceptionInWrapper(token, ex, wrapper);
                return;
            }
            if (persistentClientInstance.getUser().getId() != PermissionsManager
                    .get().getUserId() && !token.isIgnoreClientAuthMismatch()) {
                if (!token.getTransformExceptionPolicy()
                        .ignoreClientAuthMismatch(persistentClientInstance,
                                request)) {
                    DomainTransformException ex = new DomainTransformException(
                            "Browser login mismatch with transform request authentication");
                    ex.setType(
                            DomainTransformExceptionType.INVALID_AUTHENTICATION);
                    putExceptionInWrapper(token, ex, wrapper);
                    return;
                }
            }
            tm.setClientInstance(persistentClientInstance);
            if (locatorMap != null && token.isPossiblyReconstitueLocalIdMap()
                    && locatorMap.isEmpty()) {
                tm.reconstituteHiliMap();
            }
            List<DomainTransformRequest> dtrs = new ArrayList<DomainTransformRequest>();
            dtrs.addAll(request.getPriorRequestsWithoutResponse());
            dtrs.add(request);
            Integer highestPersistedRequestId = null;
            // legacy - repair incorrect client behaviour
            boolean persistRequestsBeforeHightestPersisted = ResourceUtilities
                    .getBoolean(getClass(),
                            "persistRequestsBeforeHightestPersisted");
            if (!persistRequestsBeforeHightestPersisted
                    && token.isPossiblyReconstitueLocalIdMap()) {
                highestPersistedRequestId = getHighestPersistedRequestIdForClientInstance(
                        request.getClientInstance().getId());
                for (int i = dtrs.size() - 1; i >= 0; i--) {
                    DomainTransformRequest dtr = dtrs.get(i);
                    if (highestPersistedRequestId != null && dtr
                            .getRequestId() <= highestPersistedRequestId) {
                        dtrs.remove(i);
                    }
                }
            }
            if (token.getPass() == Pass.TRY_COMMIT) {
                EntityLayerObjects.get().getMetricLogger().info(String.format(
                        "domain transform - %s - clid:"
                                + "%s - rqid:%s - highestPersistedRequestId:%s",
                        persistentClientInstance.getUser().getUserName(),
                        request.getClientInstance().getId(),
                        request.getRequestId(),
                        (highestPersistedRequestId == null ? "(servlet layer)"
                                : "highestPersistedRequestId:"
                                        + highestPersistedRequestId)));
            }
            int transformCount = 0;
            boolean replaying = LooseContext
                    .getBoolean(CONTEXT_REPLAYING_FOR_LOGS);
            int requestCount = 0;
            loop_dtrs: for (DomainTransformRequest dtr : dtrs) {
                if (dtr.checkForDuplicateEvents()) {
                    System.out.println("*** duplicate create events in rqId: "
                            + dtr.getRequestId());
                }
                List<DomainTransformEvent> items = dtr.getEvents();
                List<DomainTransformEvent> eventsPersisted = new ArrayList<DomainTransformEvent>();
                if (token.getPass() == Pass.TRY_COMMIT) {
                    commonPersistenceBase.cacheEntities(items,
                            token.getTransformExceptionPolicy()
                                    .precreateMissingEntities(),
                            true);
                }
                int backupEventIdCounter = 0;
                for (DomainTransformEvent event : items) {
                    if (event.getEventId() == 0) {
                        event.setEventId(++backupEventIdCounter);
                    }
                    if (request.getEventIdsToIgnore()
                            .contains(event.getEventId())
                            || token.getIgnoreInExceptionPass()
                                    .contains(event)) {
                        continue;
                    }
                    if (token.getPass() == Pass.TRY_COMMIT) {
                        try {
                            if (event
                                    .getCommitType() == CommitType.TO_STORAGE) {
                                if (!replaying) {
                                    tm.setIgnorePropertyChangesTo(event);
                                    tm.fireDomainTransform(event);
                                    tm.setIgnorePropertyChangesTo(null);
                                }
                                eventsPersisted.add(event);
                                wrapper.remoteEventsPersisted.add(event);
                                transformCount++;
                            }
                        } catch (Exception e) {
                            DomainTransformException transformException = DomainTransformException
                                    .wrap(e, event);
                            Registry.impl(JPAImplementation.class)
                                    .interpretException(transformException);
                            TransformExceptionAction actionForException = token
                                    .getTransformExceptionPolicy()
                                    .getActionForException(transformException,
                                            token);
                            if (!actionForException.ignoreable()) {
                                throw e;
                            } else {
                                EntityLayerObjects.get().getMetricLogger()
                                        .info(String.format(
                                                ">>>Event ignored :%s\n",
                                                e.getMessage()));
                                request.getEventIdsToIgnore()
                                        .add(event.getEventId());
                                token.ignored++;
                            }
                        }
                    } else if (token
                            .getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
                        try {
                            if (System.currentTimeMillis()
                                    - determineExceptionDetailPassStartTime > (determinedExceptionCount == 0
                                            ? MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITHOUT_EXCEPTIONS
                                            : MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITH_DET_EXCEPTIONS)) {
                                break loop_dtrs;
                            }
                            tm.fireDomainTransform(event);
                            int dontFlushTilNthTransform = token
                                    .getDontFlushTilNthTransform();
                            if (transformCount >= dontFlushTilNthTransform) {
                                tm.flush();
                                token.setDontFlushTilNthTransform(
                                        dontFlushTilNthTransform + 1);
                            }
                            transformCount++;
                        } catch (Exception e) {
                            determinedExceptionCount++;
                            DomainTransformException transformException = DomainTransformException
                                    .wrap(e, event);
                            Registry.impl(JPAImplementation.class)
                                    .interpretException(transformException);
                            token.getTransformExceptions()
                                    .add(transformException);
                            possiblyAddSilentSkips(token, transformException);
                            token.getIgnoreInExceptionPass().add(event);
                            locatorMap.clear();
                            locatorMap.putAll(locatorMapClone);
                            TransformExceptionAction actionForException = token
                                    .getTransformExceptionPolicy()
                                    .getActionForException(transformException,
                                            token);
                            switch (actionForException) {
                            case IGNORE_AND_WARN:
                                System.out.println("Ignoring: ");
                                System.out.println(transformException);
                                if (transformException.getType()
                                        .isOnlyDiscoverableStepping()) {
                                    request.getEventIdsToIgnore()
                                            .add(event.getEventId());
                                    token.ignored++;
                                    token.setPass(Pass.RETRY_WITH_IGNORES);
                                }
                                break;
                            case RESOLVE:
                                break;
                            case THROW:
                                token.setPass(Pass.FAIL);
                                break;
                            default:
                                break;
                            }
                            if (!actionForException.ignoreable()) {
                                // ve must rollback
                                putExceptionInWrapper(token, transformException,
                                        wrapper);
                                throw new DeliberatelyThrownWrapperException();
                            }
                        }
                    } // commit/determine exception
                } // dtes
                dtr.updateTransformCommitType(CommitType.ALL_COMMITTED, false);
                if (token.getPass() == Pass.TRY_COMMIT) {
                    if (ResourceUtilities.is(TransformPersister.class,
                            "flushWithEveryRequest")) {
                        entityManager.flush();// any exceptions...here we are
                    }
                    CollectionFilter<DomainTransformEvent> filterByPolicy = new CollectionFilter<DomainTransformEvent>() {
                        @Override
                        public boolean allow(DomainTransformEvent event) {
                            return token.getTransformLoggingPolicy()
                                    .shouldPersist(event)
                                    && !request.getEventIdsToIgnore()
                                            .contains(event.getEventId());
                        }
                    };
                    eventsPersisted = CollectionFilters.filter(eventsPersisted,
                            filterByPolicy);
                    if (!eventsPersisted.isEmpty()) {
                        Class<? extends DomainTransformRequestPersistent> dtrqImpl = commonPersistenceBase
                                .getImplementation(
                                        DomainTransformRequestPersistent.class);
                        Class<? extends DomainTransformEventPersistent> dtrEvtImpl = commonPersistenceBase
                                .getImplementation(
                                        DomainTransformEventPersistent.class);
                        DomainTransformRequestPersistent dtrp = dtrqImpl
                                .newInstance();
                        tm.persist(dtrp);
                        Registry.impl(DomainTransformPersistenceEvents.class)
                                .registerPersisting(dtrp);
                        dtr.setEvents(null);
                        dtrp.wrap(dtr);
                        dtrp.setEvents(new ArrayList<DomainTransformEvent>());
                        dtr.setEvents(items);
                        dtrp.setClientInstance(persistentClientInstance);
                        dtrps.add(dtrp);
                        boolean missingClassRefWarned = false;
                        for (DomainTransformEvent event : eventsPersisted) {
                            DomainTransformEventPersistent dtep = dtrEvtImpl
                                    .newInstance();
                            tm.persist(dtep);
                            dtep.wrap(event);
                            if (dtep.getObjectClassRef() == null
                                    && !missingClassRefWarned) {
                                missingClassRefWarned = true;
                                System.out.println(
                                        "Warning - persisting transform without a classRef - "
                                                + dtep);
                            }
                            if (dtep.getObjectId() == 0) {
                                dtep.setObjectId(tm
                                        .getObject(dtep.getObjectClass(), 0,
                                                dtep.getObjectLocalId())
                                        .getId());
                            }
                            if (dtep.getValueId() == 0
                                    && dtep.getValueLocalId() != 0) {
                                dtep.setValueId(tm
                                        .getObject(dtep.getValueClass(), 0,
                                                dtep.getValueLocalId())
                                        .getId());
                            }
                            dtep.setServerCommitDate(new Date());
                            dtep.setDomainTransformRequestPersistent(dtrp);
                            dtrp.getEvents().add(dtep);
                            dtreps.add(dtep);
                        }
                        if (++requestCount % 100 == 0) {
                            System.out.format(
                                    "Large rq count transform - %s/%s\n",
                                    requestCount, dtrs.size());
                        }
                    }
                } // dtes
            } // dtrs
            switch (token.getPass()) {
            case TRY_COMMIT:
                tm.flush();
                DomainTransformResponse dtr = new DomainTransformResponse();
                dtr.getEventsToUseForClientUpdate()
                        .addAll(token.getClientUpdateEvents());
                dtr.getEventsToUseForClientUpdate()
                        .addAll(tm.getModificationEvents());
                dtr.setRequestId(request.getRequestId());
                dtr.setTransformsProcessed(transformCount);
                wrapper.response = dtr;
                return;
            case RETRY_WITH_IGNORES:
                return;
            default:
                locatorMap.clear();
                locatorMap.putAll(locatorMapClone);
                token.setPass(Pass.FAIL);
                // ve must rollback
                putExceptionInWrapper(token, null, wrapper);
                throw new DeliberatelyThrownWrapperException();
            }
        } catch (Exception e) {
            if (e instanceof DeliberatelyThrownWrapperException) {
                throw (DeliberatelyThrownWrapperException) e;
            }
            if (e instanceof OptimisticLockException) {
                Object entity = ((OptimisticLockException) e).getEntity();
                if (entity != null && entity instanceof HasIdAndLocalId) {
                    System.out.format("Conflicting entity:\n\t%s\n",
                            Registry.impl(JPAImplementation.class)
                                    .entityDebugString(entity));
                }
            }
            e.printStackTrace();
            locatorMap.clear();
            locatorMap.putAll(locatorMapClone);
            if (token.getPass() == Pass.TRY_COMMIT) {
                token.setPass(Pass.DETERMINE_EXCEPTION_DETAIL);
                determineExceptionDetailPassStartTime = System
                        .currentTimeMillis();
                EntityLayerObjects.get().getMetricLogger().warn(
                        "TransformPersister: determining exception detail");
            } else {
                token.setPass(Pass.FAIL);
            }
            putExceptionInWrapper(token, e, wrapper);
            // necessary -- rollback
            throw new DeliberatelyThrownWrapperException();
        } finally {
            PermissionsManager.get().setUser(incomingUser);
        }
    }

    private class DeliberatelyThrownWrapperException extends RuntimeException {
    }

    public static void persistingTransforms() {
        GlobalTopicPublisher.get().publishTopic(TOPIC_PERSISTING_TRANSFORMS,
                Thread.currentThread());
    }

    public static void persistingTransformsListenerDelta(
            TopicListener<Thread> listener, boolean add) {
        GlobalTopicPublisher.get().listenerDelta(TOPIC_PERSISTING_TRANSFORMS,
                listener, add);
    }
}
