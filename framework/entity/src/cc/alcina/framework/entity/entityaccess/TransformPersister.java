package cc.alcina.framework.entity.entityaccess;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.HiliLocatorMap;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken.Pass;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy.TransformExceptionAction;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.util.EntityUtils;
import cc.alcina.framework.entity.util.Multiset;

public class TransformPersister {
	private static final String PRECACHE_ENTITIES = "precache entities";

	private static final String FLUSH_TRANSFORMS = "flush transforms";

	private static final String PERSIST_TRANSFORMS = "persist transforms";

	private static final String TRANSFORM_FIRE = "transform - fire";

	// note - this'll be the stack depth of the eql ast processor
	private static final int PRECACHE_RQ_SIZE = 250;

	private EntityManager entityManager;

	private CommonPersistenceLocal commonPersistenceBase;

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	public DomainTransformLayerWrapper transformExPersistenceContext(
			TransformPersistenceToken token) {
		DomainTransformLayerWrapper wrapper = null;
		while (wrapper == null
				|| token.getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
			try {
				wrapper = EntityLayerLocator.get().commonPersistenceProvider()
						.getCommonPersistence()
						.transformInPersistenceContext(this, token);
			} catch (RuntimeException ex) {
				DeliberatelyThrownWrapperException dtwe = null;
				if (ex instanceof DeliberatelyThrownWrapperException) {
					dtwe = (DeliberatelyThrownWrapperException) ex;
				} else if (ex.getCause() instanceof DeliberatelyThrownWrapperException) {
					dtwe = (DeliberatelyThrownWrapperException) ex.getCause();
				} else {
					throw ex;
				}
				wrapper = dtwe.wrapper;
			}
			if (token.getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
				token.getRequest().updateTransformCommitType(
						CommitType.TO_STORAGE, true);
				DomainTransformException firstException = token
						.getTransformExceptions().get(0);
				if (firstException.irresolvable()) {
					break;
				}
			} else if (token.getPass() == Pass.RETRY_WITH_IGNORES) {
				token.setPass(Pass.TRY_COMMIT);
				wrapper = null;
			}
		}
		if (wrapper.response.getResult() == DomainTransformResponseResult.FAILURE) {
			EntityLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().expandExceptionInfo(wrapper);
		}
		return wrapper;
	}

	private DomainTransformLayerWrapper wrapException(
			TransformPersistenceToken token, Exception e) {
		if (e != null) {
			DomainTransformException transformException;
			if (e instanceof DomainTransformException) {
				transformException = (DomainTransformException) e;
			} else {
				transformException = new DomainTransformException(e);
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
		DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
		wrapper.response = response;
		return wrapper;
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

	private Integer getLastTransformId(long clientInstanceId, int firstRequestId) {
		String eql = String
				.format("select max(dtrq.requestId) as maxId "
						+ "from %s dtrq where dtrq.clientInstance.id=%s ",
						commonPersistenceBase.getImplementation(
								DomainTransformRequestPersistent.class)
								.getSimpleName(), clientInstanceId,
						firstRequestId);
		Integer result = (Integer) getEntityManager().createQuery(eql)
				.getSingleResult();
		return result;
	}

	/**
	 * Note - parameter <em>fixWithPrecreate</em> will only be used in db
	 * replays for dbs which are somehow missing transform events
	 */
	private void preCacheEntities(List<DomainTransformEvent> items,
			boolean fixWithPrecreate) {
		Multiset<Class, Set<Long>> lkp = new Multiset<Class, Set<Long>>();
		Multiset<Class, Set<Long>> creates = new Multiset<Class, Set<Long>>();
		long maxEventId = 0;
		Date precreDate = new Date();
		for (DomainTransformEvent dte : items) {
			if (dte.getObjectId() != 0) {
				lkp.add(dte.getObjectClass(), dte.getObjectId());
				if (dte.getTransformType() == TransformType.CREATE_OBJECT) {
					creates.add(dte.getObjectClass(), dte.getObjectId());
				}
			}
			if (dte.getValueId() != 0) {
				lkp.add(dte.getValueClass(), dte.getValueId());
			}
			maxEventId = Math.max(dte.getEventId(), maxEventId);
			if (CommonUtils.compareWithNullMinusOne(precreDate,
					dte.getUtcDate()) > 0) {
				precreDate = dte.getUtcDate();
			}
		}
		for (Entry<Class, Set<Long>> entry : lkp.entrySet()) {
			Class storageClass = null;
			Class clazz = entry.getKey();
			if (clazz == null) {
				continue; // early, incorrect data - can be removed
			}
			if (clazz.getAnnotation(Entity.class) != null) {
				storageClass = clazz;
			}
			if (WrapperPersistable.class.isAssignableFrom(clazz)) {
				storageClass = commonPersistenceBase
						.getImplementation(WrappedObject.class);
			}
			if (storageClass != null) {
				List<Long> ids = new ArrayList<Long>(entry.getValue());
				for (int i = 0; i < ids.size(); i += PRECACHE_RQ_SIZE) {
					List<Long> idsSlice = ids.subList(i,
							Math.min(ids.size(), i + PRECACHE_RQ_SIZE));
					List<HasIdAndLocalId> resultList = getEntityManager()
							.createQuery(
									String.format("from %s where id in %s",
											storageClass.getSimpleName(),
											EntityUtils
													.longsToIdClause(idsSlice)))
							.getResultList();
					if (fixWithPrecreate) {
						for (HasIdAndLocalId hili : resultList) {
							entry.getValue().remove(hili.getId());
						}
					}
				}
				if (fixWithPrecreate && storageClass == clazz) {
					entry.getValue().removeAll(creates.getAndEnsure(clazz));
					for (Long lv : entry.getValue()) {
						System.out.println(String.format(
								"tp: create object: %10s %s", lv,
								clazz.getSimpleName()));
						ThreadlocalTransformManager.get().newInstance(clazz,
								lv, 0);
					}
				}
			}
		}
	}

	long determineExceptionDetailPassStartTime = 0;

	private int determinedExceptionCount;

	private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITH_DET_EXCEPTIONS = 20 * 1000;

	private static final long MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITHOUT_EXCEPTIONS = 40 * 1000;

	public DomainTransformLayerWrapper transformInPersistenceContext(
			TransformPersistenceToken token,
			CommonPersistenceBase commonPersistenceBase,
			EntityManager entityManager) {
		this.entityManager = entityManager;
		this.commonPersistenceBase = commonPersistenceBase;
		IUser incomingUser = PermissionsManager.get().getUser();
		commonPersistenceBase.connectPermissionsManagerToLiveObjects(true);
		HiliLocatorMap locatorMap = token.getLocatorMap();
		HiliLocatorMap locatorMapClone = (HiliLocatorMap) locatorMap.clone();
		DomainTransformRequest request = token.getRequest();
		List<DomainTransformEventPersistent> dtreps = new ArrayList<DomainTransformEventPersistent>();
		try {
			ObjectPersistenceHelper.get();
			ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
			// We know this is thread-local, so we can clear the tm transforms
			// add the entity version checker now
			tm.resetTltm(locatorMap, token.getTransformExceptionPolicy());
			tm.setEntityManager(getEntityManager());
			ClientInstance persistentClientInstance = (ClientInstance) commonPersistenceBase
					.findImplInstance(ClientInstance.class, request
							.getClientInstance().getId());
			if (persistentClientInstance == null
					|| (persistentClientInstance.getAuth() != null && !(persistentClientInstance
							.getAuth().equals(request.getClientInstance()
							.getAuth())))) {
				DomainTransformException ex = new DomainTransformException(
						"Invalid client instance authentication");
				ex.setType(DomainTransformExceptionType.INVALID_AUTHENTICATION);
				return wrapException(token, ex);
			}
			if (persistentClientInstance.getUser().getId() != PermissionsManager
					.get().getUserId() && !token.isIgnoreClientAuthMismatch()) {
				DomainTransformException ex = new DomainTransformException(
						"Browser login mismatch with transform request authentication");
				ex.setType(DomainTransformExceptionType.INVALID_AUTHENTICATION);
				return wrapException(token, ex);
			}
			tm.setClientInstance(persistentClientInstance);
			if (locatorMap != null && token.isPossiblyReconstitueLocalIdMap()
					&& locatorMap.isEmpty()) {
				tm.reconstituteHiliMap();
			}
			Integer lastTransformId = getLastTransformId(request
					.getClientInstance().getId(), request.getRequestId());
			List<DomainTransformRequest> dtrs = new ArrayList<DomainTransformRequest>();
			dtrs.addAll(request.getPriorRequestsWithoutResponse());
			dtrs.add(request);
			for (int i = dtrs.size() - 1; i >= 0; i--) {
				DomainTransformRequest dtr = dtrs.get(i);
				if (lastTransformId != null
						&& dtr.getRequestId() <= lastTransformId) {
					dtrs.remove(i);
				}
			}
			if (token.getPass() == Pass.TRY_COMMIT) {
				EntityLayerLocator
						.get()
						.getMetricLogger()
						.info(String.format("domain transform - %s - clid:"
								+ "%s - rqid:%s - lasttransid:%s",
								persistentClientInstance.getUser()
										.getUserName(), request
										.getClientInstance().getId(), request
										.getRequestId(), lastTransformId));
			}
			int transformCount = 0;
			loop_dtrs: for (DomainTransformRequest dtr : dtrs) {
				List<DomainTransformEvent> items = dtr.getEvents();
				List<DomainTransformEvent> eventsPersisted = new ArrayList<DomainTransformEvent>();
				if (token.getPass() == Pass.TRY_COMMIT) {
					MetricLogging.get().lowPriorityStart(PRECACHE_ENTITIES);
					preCacheEntities(items, token.getTransformExceptionPolicy()
							.precreateMissingEntities());
					MetricLogging.get().lowPriorityEnd(PRECACHE_ENTITIES);
				}
				MetricLogging.get().lowPriorityStart(TRANSFORM_FIRE);
				for (DomainTransformEvent event : items) {
					if (request.getEventIdsToIgnore().contains(
							event.getEventId())
							|| token.getIgnoreInExceptionPass().contains(event)) {
						continue;
					}
					if (token.getPass() == Pass.TRY_COMMIT) {
						try {
							if (event.getCommitType() == CommitType.TO_STORAGE) {
								tm.setIgnorePropertyChangesTo(event);
								tm.fireDomainTransform(event);
								tm.setIgnorePropertyChangesTo(null);
								eventsPersisted.add(event);
								transformCount++;
							}
						} catch (Exception e) {
							DomainTransformException transformException = DomainTransformException
									.wrap(e, event);
							EntityLayerLocator.get().jpaImplementation()
									.interpretException(transformException);
							TransformExceptionAction actionForException = token
									.getTransformExceptionPolicy()
									.getActionForException(transformException,
											token);
							if (!actionForException.ignoreable()) {
								throw e;
							} else {
								EntityLayerLocator
										.get()
										.getMetricLogger()
										.info(String.format(
												">>>Event ignored :%s\n",
												e.getMessage()));
								request.getEventIdsToIgnore().add(
										event.getEventId());
								token.ignored++;
							}
						}
					} else if (token.getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
						try {
							if (System.currentTimeMillis()
									- determineExceptionDetailPassStartTime > (determinedExceptionCount == 0 ? MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITHOUT_EXCEPTIONS
										: MAX_DURATION_DETERMINE_EXCEPTION_PASS_WITH_DET_EXCEPTIONS)) {
								break loop_dtrs;
							}
							tm.fireDomainTransform(event);
							int dontFlushTilNthTransform = token
									.getDontFlushTilNthTransform();
							if (transformCount >= dontFlushTilNthTransform) {
								getEntityManager().flush();
								token.setDontFlushTilNthTransform(dontFlushTilNthTransform + 1);
							}
							transformCount++;
						} catch (Exception e) {
							determinedExceptionCount++;
							DomainTransformException transformException = DomainTransformException
									.wrap(e, event);
							EntityLayerLocator.get().jpaImplementation()
									.interpretException(transformException);
							token.getTransformExceptions().add(
									transformException);
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
									request.getEventIdsToIgnore().add(
											event.getEventId());
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
								MetricLogging.get().lowPriorityEnd(
										TRANSFORM_FIRE);
								// ve must rollback
								throw new DeliberatelyThrownWrapperException(
										wrapException(token, transformException));
							}
						}
					}// commit/determine exception
				}// dtes
				MetricLogging.get().lowPriorityEnd(TRANSFORM_FIRE);
				MetricLogging.get().lowPriorityStart(PERSIST_TRANSFORMS);
				dtr.updateTransformCommitType(CommitType.ALL_COMMITTED, false);
				if (token.isPersistTransforms()
						&& token.getPass() == Pass.TRY_COMMIT) {
					Class<? extends DomainTransformRequestPersistent> dtrqImpl = commonPersistenceBase
							.getImplementation(DomainTransformRequestPersistent.class);
					Class<? extends DomainTransformEventPersistent> dtrEvtImpl = commonPersistenceBase
							.getImplementation(DomainTransformEventPersistent.class);
					DomainTransformRequestPersistent dtrp = dtrqImpl
							.newInstance();
					getEntityManager().persist(dtrp);
					dtr.setEvents(null);
					dtrp.wrap(dtr);
					dtrp.setEvents(new ArrayList<DomainTransformEvent>());
					dtr.setEvents(items);
					dtrp.setClientInstance(persistentClientInstance);
					for (DomainTransformEvent event : eventsPersisted) {
						if (request.getEventIdsToIgnore().contains(
								event.getEventId())) {
							continue;
						}
						DomainTransformEventPersistent dtep = dtrEvtImpl
								.newInstance();
						getEntityManager().persist(dtep);
						dtep.wrap(event);
						if (dtep.getObjectId() == 0) {
							dtep.setObjectId(tm.getObject(
									dtep.getObjectClass(), 0,
									dtep.getObjectLocalId()).getId());
						}
						if (dtep.getValueId() == 0
								&& dtep.getValueLocalId() != 0) {
							dtep.setValueId(tm.getObject(dtep.getValueClass(),
									0, dtep.getValueLocalId()).getId());
						}
						dtep.setServerCommitDate(new Date());
						dtep.setDomainTransformRequestPersistent(dtrp);
						dtrp.getEvents().add(dtep);
						dtreps.add(dtep);
					}
				}// dtes
				MetricLogging.get().lowPriorityEnd(PERSIST_TRANSFORMS);
			}// dtrs
			switch (token.getPass()) {
			case TRY_COMMIT:
				MetricLogging.get().lowPriorityStart(FLUSH_TRANSFORMS);
				try {
					getEntityManager().flush();
				} finally {
					MetricLogging.get().lowPriorityEnd(FLUSH_TRANSFORMS);
				}
				DomainTransformResponse dtr = new DomainTransformResponse();
				dtr.getEventsToUseForClientUpdate().addAll(
						token.getClientUpdateEvents());
				dtr.getEventsToUseForClientUpdate().addAll(
						tm.getModificationEvents());
				dtr.setRequestId(request.getRequestId());
				dtr.setTransformsProcessed(transformCount);
				DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
				wrapper.locatorMap = locatorMap;
				wrapper.response = dtr;
				wrapper.persistentEvents = dtreps;
				return wrapper;
			case RETRY_WITH_IGNORES:
				return null;
			default:
				locatorMap.clear();
				locatorMap.putAll(locatorMapClone);
				token.setPass(Pass.FAIL);
				// ve must rollback
				throw new DeliberatelyThrownWrapperException(wrapException(
						token, null));
			}
		} catch (Exception e) {
			if (e instanceof DeliberatelyThrownWrapperException) {
				throw (DeliberatelyThrownWrapperException) e;
			}
			e.printStackTrace();
			locatorMap.clear();
			locatorMap.putAll(locatorMapClone);
			if (token.getPass() == Pass.TRY_COMMIT) {
				token.setPass(Pass.DETERMINE_EXCEPTION_DETAIL);
				determineExceptionDetailPassStartTime = System
						.currentTimeMillis();
				EntityLayerLocator
						.get()
						.getMetricLogger()
						.warn("TransformPersister: determining exception detail");
			} else {
				token.setPass(Pass.FAIL);
			}
			return wrapException(token, e);
		} finally {
			PermissionsManager.get().setUser(incomingUser);
		}
	}

	private class DeliberatelyThrownWrapperException extends RuntimeException {
		private final DomainTransformLayerWrapper wrapper;

		public DeliberatelyThrownWrapperException(
				DomainTransformLayerWrapper wrapper) {
			this.wrapper = wrapper;
		}
	}
}
