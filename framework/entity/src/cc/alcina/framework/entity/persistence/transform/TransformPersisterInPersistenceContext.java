package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.persistence.WrappedObject;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.TransformPersistenceToken.Pass;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.policy.PersistenceLayerTransformExceptionPolicy.TransformExceptionAction;
import cc.alcina.framework.entity.transform.policy.TransformPropagationPolicy;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class TransformPersisterInPersistenceContext {
	public static final String CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM";

	public static final String CONTEXT_REPLAYING_FOR_LOGS = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_REPLAYING_FOR_LOGS";

	public static final String CONTEXT_LOG_TO_STDOUT = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_LOG_TO_STDOUT";

	public static final String CONTEXT_DO_NOT_PERSIST_TRANSFORMS = TransformPersisterInPersistenceContext.class
			.getName() + ".CONTEXT_DO_NOT_PERSIST_TRANSFORMS";

	private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITH_DET_EXCEPTIONS = 20
			* 1000;

	private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITHOUT_EXCEPTIONS = 40
			* 1000;

	static void putExceptionInWrapper(TransformPersistenceToken token,
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
		final DomainTransformRequest request = token.getRequest();
		List<DomainTransformEventPersistent> persistentEvents = wrapper.persistentEvents;
		List<DomainTransformRequestPersistent> dtrps = wrapper.persistentRequests;
		wrapper.locatorMap = locatorMap;
		ObjectPersistenceHelper.get();
		ThreadlocalTransformManager tlTransformManager = ThreadlocalTransformManager
				.cast();
		DelayedEntityPersister delayedEntityPersister = new DelayedEntityPersister();
		List<DomainTransformRequest> transformRequests = new ArrayList<DomainTransformRequest>();
		try {
			// We know this is thread-local, so we can clear the tm transforms
			// add the entity version checker now
			tlTransformManager.resetTltm(locatorMap,
					token.getTransformExceptionPolicy(), true);
			tlTransformManager.setEntityManager(getEntityManager());
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
			tlTransformManager.setClientInstance(persistentClientInstance);
			tlTransformManager.setUseCreatedLocals(false);
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
			if (token.isRequestorExternalToThisJvm()) {
				highestPersistedRequestId = commonPersistenceBase
						.getHighestPersistedRequestIdForClientInstance(
								request.getClientInstance().getId());
				for (int i = transformRequests.size() - 1; i >= 0; i--) {
					DomainTransformRequest dtr = transformRequests.get(i);
					if (highestPersistedRequestId != null && dtr
							.getRequestId() <= highestPersistedRequestId) {
						Ax.out("transformpersister - removing already processed "
								+ "request :: %s/%s",
								request.getClientInstance().getId(),
								transformRequests.get(i).getRequestId());
						transformRequests.remove(i);
					}
				}
			}
			if (token.getPass() == Pass.TRY_COMMIT) {
				EntityLayerObjects.get().getMetricLogger().debug(String.format(
						"domain transform - %s - %s/%s",
						persistentClientInstance.provideUser().getUserName(),
						request.getClientInstance().getId(),
						transformRequests.stream()
								.map(DomainTransformRequest::getRequestId)
								.map(String::valueOf)
								.collect(Collectors.joining(","))));
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
									persistEvent(tlTransformManager,
											delayedEntityPersister, event);
									if (tlTransformManager
											.provideIsMarkedFlushTransform(
													event)) {
										tlTransformManager.flush();
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
							tlTransformManager.fireDomainTransform(event);
							int dontFlushTilNthTransform = token
									.getDontFlushTilNthTransform();
							if (transformCount >= dontFlushTilNthTransform) {
								tlTransformManager.flush();
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
							undoLocatorMapDeltas(locatorMap, transformRequests);
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
				delayedEntityPersister.checkPersistEntity(null);
				subRequest.updateTransformCommitType(CommitType.ALL_COMMITTED,
						false);
				if (token.getPass() == Pass.TRY_COMMIT) {
					// if (ResourceUtilities.is(TransformPersister.class,
					// "flushWithEveryRequest")) {
					// // defaults to true - remember this isn't committing -
					// // isn't much of a speed bump given significant requests
					// // are almost always server-side, non-'with unpublished'
					// /*
					// * Nice thing about this is that it means no dtrp if
					// * there are any issues
					// */
					// entityManager.flush();
					// }
					// in fact...it doubles the db commit time. Removed
					if (request.getEventIdsToIgnore().size() > 0) {
						eventsPersisted
								.removeIf(event -> request.getEventIdsToIgnore()
										.contains(event.getEventId()));
					}
					if (LooseContext.is(
							TransformPersister.CONTEXT_DO_NOT_PERSIST_DTES)) {
						eventsPersisted.clear();
					}
					if (!eventsPersisted.isEmpty()) {
						TransformPropagationPolicy propagationPolicy = token
								.getTransformPropagationPolicy();
						Class<? extends DomainTransformRequestPersistent> persistentRequestClass = PersistentImpl
								.getImplementation(
										DomainTransformRequestPersistent.class);
						Class<? extends DomainTransformEventPersistent> persistentEventClass = PersistentImpl
								.getImplementation(
										DomainTransformEventPersistent.class);
						DomainTransformRequestPersistent persistentRequest = persistentRequestClass
								.newInstance();
						tlTransformManager.persist(persistentRequest);
						persistentRequest.setStartPersistTime(startPersistTime);
						if (!LooseContext.is(
								CONTEXT_NOT_REALLY_SERIALIZING_ON_THIS_VM)) {
							DomainStore.stores().writableStore()
									.getPersistenceEvents().getQueue()
									.onPersistingVmLocalRequest(
											persistentRequest);
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
						AtomicBoolean missingClassRefWarned = new AtomicBoolean();
						/*
						 * Make sure collation and locator refs match up
						 */
						eventsPersisted.forEach(event -> {
							if (event.getObjectId() == 0) {
								event.setObjectId(tlTransformManager
										.getClientInstanceEntityMap()
										.getForLocalId(event.getObjectLocalId())
										.getId());
							}
						});
						new PersistentEventPopulator().populate(
								persistentEvents, tlTransformManager,
								eventsPersisted, propagationPolicy,
								persistentEventClass, persistentRequest,
								missingClassRefWarned,
								LooseContext.is(
										TransformPersisterInPersistenceContext.CONTEXT_DO_NOT_PERSIST_TRANSFORMS),
								false);
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
				DomainTransformResponse response = new DomainTransformResponse();
				response.getEventsToUseForClientUpdate()
						.addAll(token.getClientUpdateEvents());
				response.getEventsToUseForClientUpdate()
						.addAll(tlTransformManager.getModificationEvents());
				response.setRequestId(request.getRequestId());
				response.setTransformsProcessed(transformCount);
				wrapper.response = response;
				DomainStore.writableStore().getPersistenceEvents()
						.fireDomainTransformPersistenceEvent(
								new DomainTransformPersistenceEvent(token,
										wrapper,
										DomainTransformPersistenceEventType.PRE_FLUSH,
										true));
				return;
			case RETRY_WITH_IGNORES:
				return;
			default:
				undoLocatorMapDeltas(locatorMap, transformRequests);
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
			undoLocatorMapDeltas(locatorMap, transformRequests);
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
		} finally {
			tlTransformManager.setUseCreatedLocals(true);
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

	private void undoLocatorMapDeltas(EntityLocatorMap locatorMap,
			List<DomainTransformRequest> transformRequests) {
		transformRequests.stream().map(DomainTransformRequest::getEvents)
				.flatMap(Collection::stream)
				.filter(dte -> dte.provideIsCreationTransform())
				.map(DomainTransformEvent::toObjectLocator)
				.forEach(locator -> locatorMap.remove(locator));
	}

	protected void persistEvent(ThreadlocalTransformManager tlTransformManager,
			DelayedEntityPersister delayedEntityPersister,
			DomainTransformEvent event) throws DomainTransformException {
		boolean wrappedObjectAssignable = WrappedObject.class
				.isAssignableFrom(event.getObjectClass());
		// do not apply parent association transforms (although they'll be used
		// in domainstore processing)
		switch (event.getTransformType()) {
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			OneToMany oneToMany = Reflections.classLookup()
					.getPropertyReflector(event.getObjectClass(),
							event.getPropertyName())
					.getAnnotation(OneToMany.class);
			if (oneToMany != null) {
				/*
				 * Ensure the source/target object exist (otherwise there'll be
				 * an invalid attempt to apply to the graph). But ignore for db
				 * persistence
				 */
				if (tlTransformManager
						.getObject(event.toObjectLocator()) == null) {
					throw new DomainTransformException(event,
							DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND);
				}
				if (tlTransformManager
						.getObject(event.toValueLocator()) == null) {
					throw new DomainTransformException(event,
							DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND);
				}
				return;
			}
			break;
		}
		try {
			if (wrappedObjectAssignable) {
				tlTransformManager.setIgnorePropertyChangesTo(event);
			} else {
				tlTransformManager.setIgnorePropertyChanges(true);
			}
			if (event.getNewStringValue() != null
					&& event.getNewStringValue().contains("\u0000")) {
				logger.warn("Removed unicode 0x0 from event {}/{}/{}",
						event.toObjectLocator(), event.getTransformType(),
						event.getPropertyName());
				// pg will not accept 0x0
				event.setNewStringValue(
						event.getNewStringValue().replace("\u0000", ""));
			}
			tlTransformManager.fireDomainTransform(event);
			delayedEntityPersister.checkPersistEntity(event);
		} finally {
			if (wrappedObjectAssignable) {
				tlTransformManager.setIgnorePropertyChangesTo(null);
			} else {
				tlTransformManager.setIgnorePropertyChanges(false);
			}
		}
	}

	/*
	 * Most insertion transform patterns will be
	 * "create - modify modify modify - next entity" - this persistence approach
	 * means no need to worry about persisting associations of not-yet-persisted
	 * entities
	 */
	private class DelayedEntityPersister {
		DomainTransformEvent lastCreationEvent = null;

		Set<Long> persistedLocals = new LongOpenHashSet(
				Hash.DEFAULT_INITIAL_SIZE, Hash.VERY_FAST_LOAD_FACTOR);

		void checkPersistEntity(DomainTransformEvent event) {
			if (lastCreationEvent != null
					&& (event == null || event.getObjectId() != 0
							|| event.getObjectLocalId() != lastCreationEvent
									.getObjectLocalId())) {
				Entity entity = ThreadlocalTransformManager.cast()
						.getLocalIdToEntityMap()
						.get(lastCreationEvent.getObjectLocalId());
				getEntityManager().persist(entity);
				persistedLocals.add(lastCreationEvent.getObjectLocalId());
				lastCreationEvent.setGeneratedServerId(entity.getId());
				ThreadlocalTransformManager.cast().getClientInstanceEntityMap()
						.putToLookups(entity.toLocator());
				lastCreationEvent = null;
			}
			if (event != null
					&& event.getTransformType() == TransformType.CREATE_OBJECT
					&& !persistedLocals.contains(event.getObjectLocalId())) {
				lastCreationEvent = event;
			}
		}
	}

	static class DeliberatelyThrownWrapperException extends RuntimeException {
	}
}
