package cc.alcina.framework.entity.entityaccess.transform;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken.Pass;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy.TransformExceptionAction;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.logic.EntityLayerObjects;

/**
 * 
 * @author nick@alcina.cc
 *
 */
public class TransformPersisterInPersistenceContext {
	public static final String CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM";

	public static final String CONTEXT_REPLAYING_FOR_LOGS = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_REPLAYING_FOR_LOGS";

	public static final String CONTEXT_LOG_TO_STDOUT = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_LOG_TO_STDOUT";

	private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITH_DET_EXCEPTIONS = 20
			* 1000;

	private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITHOUT_EXCEPTIONS = 40
			* 1000;

	private transient EntityManager entityManager;

	Logger logger = LoggerFactory.getLogger(getClass());

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	public void transformInPersistenceContext(
			TransformPersisterToken transformPersisterToken,
			final TransformPersistenceToken token,
			CommonPersistenceBase commonPersistenceBase,
			EntityManager entityManager, DomainTransformLayerWrapper wrapper) {
		Date startPersistTime = new Date();
		this.entityManager = entityManager;
		EntityLocatorMap locatorMap = token.getLocatorMap();
		EntityLocatorMap locatorMapClone = (EntityLocatorMap) locatorMap.copy();
		final DomainTransformRequest request = token.getRequest();
		List<DomainTransformEventPersistent> persistentEvents = wrapper.persistentEvents;
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
			if (persistentClientInstance.provideUser()
					.getId() != PermissionsManager.get().getUserId()
					&& !token.isIgnoreClientAuthMismatch()) {
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
			List<DomainTransformRequest> transformRequests = new ArrayList<DomainTransformRequest>();
			transformRequests.addAll(request.getPriorRequestsWithoutResponse());
			transformRequests.add(request);
			for (DomainTransformRequest dtr : transformRequests) {
				if (dtr.getRequestId() == 0) {
					DomainTransformException ex = new DomainTransformException(
							Ax.format("Domain transform request with id 0: %s",
									dtr.toStringForError()));
					ex.setType(DomainTransformExceptionType.UNKNOWN);
					putExceptionInWrapper(token, ex, wrapper);
					return;
				}
			}
			Multimap<Integer, List<DomainTransformRequest>> byRequestId = transformRequests
					.stream().collect(AlcinaCollectors.toKeyMultimap(
							DomainTransformRequest::getRequestId));
			Optional<Entry<Integer, List<DomainTransformRequest>>> multipleDtrsForOneRequestId = byRequestId
					.entrySet().stream().filter(e -> e.getValue().size() > 1)
					.findFirst();
			if (multipleDtrsForOneRequestId.isPresent()) {
				DomainTransformException ex = new DomainTransformException(Ax
						.format("Multiple domain transform requests with local request id %s (%s)",
								multipleDtrsForOneRequestId.get().getKey(),
								multipleDtrsForOneRequestId.get().getValue()
										.size()));
				ex.setType(DomainTransformExceptionType.UNKNOWN);
				putExceptionInWrapper(token, ex, wrapper);
				return;
			}
			Integer highestPersistedRequestId = null;
			if (token.isAsyncClient()) {
				highestPersistedRequestId = commonPersistenceBase
						.getHighestPersistedRequestIdForClientInstance(
								request.getClientInstance().getId());
				for (int i = transformRequests.size() - 1; i >= 0; i--) {
					DomainTransformRequest dtr = transformRequests.get(i);
					if (highestPersistedRequestId != null && dtr
							.getRequestId() <= highestPersistedRequestId) {
						Ax.out("transformpersister - removing already processed "
								+ "request :: clid: %s; rqid: %s",
								request.getClientInstance().getId(),
								transformRequests.get(i).getRequestId());
						transformRequests.remove(i);
					}
				}
			}
			if (token.getPass() == Pass.TRY_COMMIT) {
				EntityLayerObjects.get().getMetricLogger().info(String.format(
						"domain transform - %s - clid:"
								+ "%s - rqid:%s - prev-per-cli-id:%s",
						persistentClientInstance.provideUser().getUserName(),
						request.getClientInstance().getId(),
						transformRequests.stream()
								.map(DomainTransformRequest::getRequestId)
								.map(String::valueOf)
								.collect(Collectors.joining(",")),
						(highestPersistedRequestId == null ? "(servlet layer)"
								: highestPersistedRequestId)));
			}
			int transformCount = 0;
			boolean replaying = LooseContext
					.getBoolean(CONTEXT_REPLAYING_FOR_LOGS);
			int requestCount = 0;
			loop_dtrs: for (DomainTransformRequest subRequest : transformRequests) {
				if (subRequest.checkForDuplicateEvents()) {
					System.out.println("*** duplicate create events in rqId: "
							+ subRequest.getRequestId());
				}
				List<DomainTransformEvent> events = subRequest.getEvents();
				List<DomainTransformEvent> eventsPersisted = new ArrayList<DomainTransformEvent>();
				if (token.getPass() == Pass.TRY_COMMIT) {
					commonPersistenceBase.cacheEntities(events,
							token.getTransformExceptionPolicy()
									.precreateMissingEntities(),
							true);
					if (LooseContext.is(CONTEXT_LOG_TO_STDOUT)) {
						Ax.out(subRequest);
					}
				}
				int backupEventIdCounter = 0;
				for (DomainTransformEvent event : events) {
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
									if (tm.provideIsMarkedFlushTransform(
											event)) {
										tm.flush();
									}
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
									- transformPersisterToken.determineExceptionDetailPassStartTime > (transformPersisterToken.determinedExceptionCount == 0
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
							transformPersisterToken.determinedExceptionCount++;
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
				subRequest.updateTransformCommitType(CommitType.ALL_COMMITTED,
						false);
				if (token.getPass() == Pass.TRY_COMMIT) {
					if (ResourceUtilities.is(TransformPersister.class,
							"flushWithEveryRequest")) {
						// defaults to true - remember this isn't committing -
						// isn't much of a speed bump given significant requests
						// are almost always server-side, non-'with unpublished'
						/*
						 * Nice thing about this is that it means no dtrp if
						 * there are any issues
						 */
						entityManager.flush();
					}
					CollectionFilter<DomainTransformEvent> filterByPolicy = new CollectionFilter<DomainTransformEvent>() {
						@Override
						public boolean allow(DomainTransformEvent event) {
							return token.getTransformLoggingPolicy()
									.shouldPersist(event)
									&& !request.getEventIdsToIgnore()
											.contains(event.getEventId())
									&& !LooseContext.is(
											TransformPersister.CONTEXT_DO_NOT_PERSIST_DTES);
						}
					};
					eventsPersisted = CollectionFilters.filter(eventsPersisted,
							filterByPolicy);
					if (!eventsPersisted.isEmpty()) {
						Class<? extends DomainTransformRequestPersistent> dtrqImpl = AlcinaPersistentEntityImpl
								.getImplementation(
										DomainTransformRequestPersistent.class);
						Class<? extends DomainTransformEventPersistent> dtrEvtImpl = AlcinaPersistentEntityImpl
								.getImplementation(
										DomainTransformEventPersistent.class);
						DomainTransformRequestPersistent persistentRequest = dtrqImpl
								.newInstance();
						tm.persist(persistentRequest);
						Calendar defaultCalendar = Calendar.getInstance();
						int offset = defaultCalendar.getTimeZone()
								.getOffset(startPersistTime.getTime());
						Timestamp utcStartPersistTime = new Timestamp(
								startPersistTime.getTime() - offset);
						persistentRequest
								.setStartPersistTime(utcStartPersistTime);
						if (!LooseContext.is(
								CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM)) {
							DomainStore.stores().writableStore()
									.getPersistenceEvents().getQueue()
									.registerPersisting(persistentRequest);
						}
						subRequest.setEvents(null);
						persistentRequest.wrap(subRequest);
						persistentRequest.setEvents(
								new ArrayList<DomainTransformEvent>());
						subRequest.setEvents(events);
						persistentRequest
								.setClientInstance(persistentClientInstance);
						persistentRequest.setOriginatingUserId(
								token.getOriginatingUserId());
						dtrps.add(persistentRequest);
						boolean missingClassRefWarned = false;
						for (DomainTransformEvent event : eventsPersisted) {
							DomainTransformEventPersistent persistentEvent = dtrEvtImpl
									.newInstance();
							boolean persistEvent = token
									.getTransformPersistencePolicy()
									.shouldPersist(event);
							if (persistEvent) {
								tm.persist(persistentEvent);
							}
							persistentEvent.wrap(event);
							if (persistentEvent.getObjectClassRef() == null
									&& !missingClassRefWarned) {
								missingClassRefWarned = true;
								System.out.println(
										"Warning - persisting transform without a classRef - "
												+ persistentEvent);
							}
							if (persistentEvent.getObjectId() == 0) {
								persistentEvent.setObjectId(tm.getObject(
										persistentEvent.getObjectClass(), 0,
										persistentEvent.getObjectLocalId())
										.getId());
							}
							if (persistentEvent.getValueId() == 0
									&& persistentEvent.getValueLocalId() != 0) {
								persistentEvent.setValueId(tm
										.getObject(
												persistentEvent.getValueClass(),
												0,
												persistentEvent
														.getValueLocalId())
										.getId());
							}
							persistentEvent.setServerCommitDate(new Date());
							if (persistEvent) {
								persistentEvent
										.setDomainTransformRequestPersistent(
												persistentRequest);
								persistentRequest.getEvents()
										.add(persistentEvent);
								persistentEvents.add(persistentEvent);
							}
						}
						if (++requestCount % 100 == 0) {
							System.out.format(
									"Large rq count transform - %s/%s\n",
									requestCount, transformRequests.size());
						}
					}
				} // dtes
			} // dtrs
			persistentEvents.forEach(
					event -> event.beforeTransformCommit(getEntityManager()));
			switch (token.getPass()) {
			case TRY_COMMIT:
				tm.flush(persistentEvents);
				DomainTransformResponse response = new DomainTransformResponse();
				response.getEventsToUseForClientUpdate()
						.addAll(token.getClientUpdateEvents());
				response.getEventsToUseForClientUpdate()
						.addAll(tm.getModificationEvents());
				response.setRequestId(request.getRequestId());
				response.setTransformsProcessed(transformCount);
				wrapper.response = response;
				return;
			case RETRY_WITH_IGNORES:
				return;
			default:
				locatorMap.clear();
				locatorMap.putAll(locatorMapClone);
				token.setPass(Pass.FAIL);
				// ve must rollback
				putExceptionInWrapper(token, null, wrapper);
				logger.warn("Underlying transform exception: ",
						CommonUtils.first(token.getTransformExceptions()));
				throw new DeliberatelyThrownWrapperException();
			}
		} catch (Exception e) {
			if (e instanceof DeliberatelyThrownWrapperException) {
				throw (DeliberatelyThrownWrapperException) e;
			}
			if (e instanceof OptimisticLockException) {
				Object entity = ((OptimisticLockException) e).getEntity();
				if (entity != null && entity instanceof Entity) {
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
				transformPersisterToken.determineExceptionDetailPassStartTime = System
						.currentTimeMillis();
				logger.warn("TransformPersister: determining exception detail");
			} else {
				token.setPass(Pass.FAIL);
			}
			putExceptionInWrapper(token, e, wrapper);
			// necessary -- rollback
			throw new DeliberatelyThrownWrapperException();
		}
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

	static class DeliberatelyThrownWrapperException extends RuntimeException {
	}
}
