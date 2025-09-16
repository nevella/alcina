package cc.alcina.framework.entity.persistence.transform;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
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
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
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

	protected void persistEvent(ThreadlocalTransformManager tlTransformManager,
			TransformPersistenceToken token,
			DelayedEntityPersister delayedEntityPersister,
			DomainTransformEvent event) throws DomainTransformException {
		try {
			tlTransformManager.setIgnorePropertyChanges(true);
			if (event.getNewStringValue() != null
					&& event.getNewStringValue().contains("\u0000")) {
				logger.warn("Removed unicode 0x0 from event {}/{}/{}",
						event.toObjectLocator(), event.getTransformType(),
						event.getPropertyName());
				// pg will not accept 0x0
				event.setNewStringValue(
						event.getNewStringValue().replace("\u0000", ""));
				// and must blank this too
				event.setNewValue(null);
			}
			tlTransformManager.fireDomainTransform(event);
			if (delayedEntityPersister.checkPersistEntity(event)) {
			} else {
				delayedEntityPersister.checkUpdateVersions(tlTransformManager,
						token, event);
			}
		} finally {
			tlTransformManager.setIgnorePropertyChanges(false);
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

	public boolean removeProcessedRequests(
			CommonPersistenceBase commonPersistenceBase,
			TransformPersistenceToken token) {
		if (token.isRequestorExternalToThisJvm()) {
			Integer highestPersistedRequestId = commonPersistenceBase
					.getHighestPersistedRequestIdForClientInstance(
							token.getRequest().getClientInstance().getId());
			if (highestPersistedRequestId == null) {
				return true;
			}
			if (token.getRequest()
					.getRequestId() <= highestPersistedRequestId) {
				return false;
			}
			List<DomainTransformRequest> toRemove = token.getRequest()
					.getPriorRequestsWithoutResponse().stream()
					.filter(request -> request
							.getRequestId() <= highestPersistedRequestId)
					.collect(Collectors.toList());
			toRemove.forEach(request -> logger.info(
					"transformpersister - removing already processed "
							+ "request :: {}/{}",
					request.getClientInstance().getId(),
					request.getRequestId()));
			token.getRequest().getPriorRequestsWithoutResponse()
					.removeAll(toRemove);
			return true;
		} else {
			return true;
		}
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
		ThreadlocalTransformManager tlTransformManager = ThreadlocalTransformManager
				.cast();
		DelayedEntityPersister delayedEntityPersister = new DelayedEntityPersister();
		List<DomainTransformRequest> transformRequests = new ArrayList<DomainTransformRequest>();
		try {
			// We know this is thread-local, so we can clear the tm transforms
			// add the entity version checker now
			tlTransformManager.resetTltm(locatorMap,
					token.getTransformExceptionPolicy(), true, true);
			token.getTransformCollation().refreshFromRequest();
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
									persistEvent(tlTransformManager, token,
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
					}
					// commit/determine exception
				}
				// dtes
				delayedEntityPersister.checkPersistEntity(null);
				subRequest.updateTransformCommitType(CommitType.ALL_COMMITTED,
						false);
				if (token.getPass() == Pass.TRY_COMMIT) {
					// if (Configuration.is(TransformPersister.class,
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
								.getDeclaredConstructor().newInstance();
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
						Long originatingUserId = token.getOriginatingUserId();
						IUser originatingUser = originatingUserId == null ? null
								: getEntityManager().find(
										PersistentImpl
												.getImplementation(IUser.class),
										originatingUserId);
						new PersistentEventPopulator().populate(originatingUser,
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
				}
				// dtes
			}
			// dtrs
			persistentEvents.forEach(
					event -> event.beforeTransformCommit(getEntityManager()));
			switch (token.getPass()) {
			case TRY_COMMIT:
				DomainTransformResponse response = new DomainTransformResponse();
				response.getEventsToUseForClientUpdate()
						.addAll(token.getClientUpdateEvents());
				response.getEventsToUseForClientUpdate()
						.addAll(tlTransformManager.getModificationEvents());
				token.initialTransforms.filter(response.getEventsToUseForClientUpdate());
				response.setRequestId(request.getRequestId());
				response.setTransformsProcessed(transformCount);
				wrapper.response = response;
				DomainStore.writableStore().getPersistenceEvents()
						.fireDomainTransformPersistenceEvent(
								new DomainTransformPersistenceEvent(token,
										wrapper,
										DomainTransformPersistenceEventType.PRE_COMMIT,
										true));
				// make sure that event didn't try and add transforms
				Preconditions.checkState(
						TransformManager.get().getTransforms().isEmpty());
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
			tlTransformManager.setEntityManager(null);
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

	private class DelayedEntityPersister {
		DomainTransformEvent lastCreationEvent = null;

		Set<Long> persistedLocals = new LongOpenHashSet(
				Hash.DEFAULT_INITIAL_SIZE, Hash.VERY_FAST_LOAD_FACTOR);

		/*
		 * Most insertion transform patterns will be
		 * "create - modify modify modify - next entity" - this persister
		 * optimises (by only writing when required) and prevents ensures
		 * entities are persisted before (incoming) association with other
		 * entities
		 */
		boolean checkPersistEntity(DomainTransformEvent event) {
			boolean updatedVersions = false;
			if (lastCreationEvent != null
					&& (event == null || event.getObjectId() != 0
							|| event.getObjectLocalId() != lastCreationEvent
									.getObjectLocalId())) {
				Entity entity = ThreadlocalTransformManager.cast()
						.getLocalIdToEntityMap()
						.get(lastCreationEvent.getObjectLocalId());
				updateVersions(entity);
				getEntityManager().persist(entity);
				persistedLocals.add(lastCreationEvent.getObjectLocalId());
				lastCreationEvent.setGeneratedServerId(entity.getId());
				ThreadlocalTransformManager.cast().getClientInstanceEntityMap()
						.putToLookups(entity.toLocator());
				lastCreationEvent = null;
				updatedVersions = true;
			}
			if (event != null
					&& event.getTransformType() == TransformType.CREATE_OBJECT
					&& !persistedLocals.contains(event.getObjectLocalId())) {
				lastCreationEvent = event;
			}
			return updatedVersions;
		}

		/*
		 * Only update version metadata if entity has a db property (i.e.
		 * non-one-to-many-collection-ref property) change
		 */
		public void checkUpdateVersions(
				ThreadlocalTransformManager tlTransformManager,
				TransformPersistenceToken token, DomainTransformEvent event) {
			if (event.provideIsDeletionTransform()) {
				return;
			}
			EntityCollation entityCollation = token.getTransformCollation()
					.forLocator(event.toObjectLocator());
			if (entityCollation == null) {
				// FIXME - mvcc.5
				logger.warn("Missing collation :: {}", event.toObjectLocator());
				return;
			}
			if (event == entityCollation.last()) {
				/*
				 *
				 */
				if (entityCollation.getTransforms().stream().anyMatch(e -> {
					switch (e.getTransformType()) {
					case CREATE_OBJECT:
					case CHANGE_PROPERTY_REF:
					case NULL_PROPERTY_REF:
					case CHANGE_PROPERTY_SIMPLE_VALUE:
						return true;
					case ADD_REF_TO_COLLECTION:
					case REMOVE_REF_FROM_COLLECTION:
						OneToMany oneToMany = Reflections.at(e.getObjectClass())
								.property(e.getPropertyName())
								.annotation(OneToMany.class);
						// if null, manytomany and it *is* an update (so null
						// value implies 'return true, was updated')
						return oneToMany == null;
					case DELETE_OBJECT:
					default:
						throw new UnsupportedOperationException();
					}
				})) {
					Entity entity = tlTransformManager
							.getObject(event.toObjectLocator());
					updateVersions(entity);
				}
			}
		}

		private void updateVersions(Entity entity) {
			if (entity instanceof VersionableEntity) {
				Date now = new Date();
				VersionableEntity versionableEntity = (VersionableEntity) entity;
				versionableEntity.setLastModificationDate(now);
				if (versionableEntity.getCreationDate() == null) {
					versionableEntity.setCreationDate(now);
				}
			}
		}
	}

	static class DeliberatelyThrownWrapperException extends RuntimeException {
	}

	@Registration.Singleton
	/*
	 * Because 'onFlushDirty' is called well before batch exceptions occur, the
	 * correct approach was a small change to hibernate and insertion of the
	 * exceptionConsumer. So 'lastFlushData' etc is unused.
	 *
	 * Buuttt...even more optimal would be
	 */
	public static class ThreadData {
		public static ThreadData get() {
			return Registry.impl(ThreadData.class);
		}

		BiConsumer<RuntimeException, PreparedStatement> exceptionConsumer = (re,
				ps) -> this.consumeException(re, ps);

		private ConcurrentMap<Thread, FlushData> lastFlushData = new ConcurrentHashMap<>();

		private ConcurrentMap<Thread, Boolean> observingFlushData = new ConcurrentHashMap<>();

		Logger logger = LoggerFactory.getLogger(getClass());

		private Class logFlushDataOfClass;

		public ThreadData() {
			// reflection-based
			if (!Ax.isTest()) {
				Registry.impl(JPAImplementation.class)
						.registerBatchExceptionConsumer(exceptionConsumer);
			}
		}

		public void afterTransactionCompletion() {
			//
			// because we may only learn about the exception (in client code)
			// after tx completion, use client observing call to evict rather
			// than here
		}

		void consumeException(RuntimeException re, PreparedStatement ps) {
			try {
				if (ps.getClass().getName().equals(
						"org.jboss.jca.adapters.jdbc.jdk8.WrappedPreparedStatementJDK8")) {
					Field wrappedField = SEUtilities
							.getFieldByName(ps.getClass(), "s");
					wrappedField.setAccessible(true);
					ps = (PreparedStatement) wrappedField.get(ps);
				}
				logger.warn("(Prepared) statement causing issue");
				logger.warn(ps.toString());
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public void logFlushDataOfClass(Class logFlushDataOfClass) {
			this.logFlushDataOfClass = logFlushDataOfClass;
		}

		public void logLastFlushData() {
			Thread currentThread = Thread.currentThread();
			FlushData flushData = lastFlushData.get(currentThread);
			if (flushData != null) {
				logger.info("Last entity flush data");
				logger.info(flushData.toString());
			}
		}

		void observingFlushData(boolean observing) {
			Thread currentThread = Thread.currentThread();
			if (observing) {
				observingFlushData.put(currentThread, true);
			} else {
				lastFlushData.remove(currentThread);
				observingFlushData.remove(currentThread);
			}
		}

		public void onFlushDirty(Object entity, Serializable id,
				Object[] currentState, Object[] previousState,
				String[] propertyNames, Object[] types) {
			Thread currentThread = Thread.currentThread();
			if (!observingFlushData.containsKey(currentThread)) {
				return;
			}
			FlushData flushData = new FlushData(entity, id, currentState,
					previousState, propertyNames, types);
			if (entity.getClass() == logFlushDataOfClass) {
				logger.info("Logging flush data: {} - {}/{}\n{}", entity,
						entity.getClass().getSimpleName(), id,
						flushData.toDelta());
			}
			lastFlushData.put(currentThread, flushData);
		}

		@SuppressWarnings("unused")
		static class FlushData {
			private Object entity;

			private Serializable id;

			private Object[] currentState;

			private Object[] previousState;

			private String[] propertyNames;

			private Object[] types;

			FlushData(Object entity, Serializable id, Object[] currentState,
					Object[] previousState, String[] propertyNames,
					Object[] types) {
				this.entity = entity;
				this.id = id;
				this.currentState = currentState;
				this.previousState = previousState;
				this.propertyNames = propertyNames;
				this.types = types;
			}

			public String toDelta() {
				FormatBuilder fb = new FormatBuilder();
				for (int idx = 0; idx < currentState.length; idx++) {
					Object p = previousState[idx];
					Object c = currentState[idx];
					Object n = propertyNames[idx];
					if (!Objects.equals(p, c)) {
						fb.line("%s :: %s => %s", n, p, c);
					}
				}
				return fb.toString();
			}

			@Override
			public String toString() {
				FormatBuilder fb = new FormatBuilder();
				fb.line("Entity: %s", entity);
				fb.line("Id: %s", id);
				return fb.toString();
			}
		}
	}
}
