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
package cc.alcina.framework.entity.persistence;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.ClientLogRecordPersistent;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.PublicationCounter;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DurationCounter;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.ObjectUtil;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence.BootstrapCreationResult;
import cc.alcina.framework.entity.persistence.domain.DomainLinker;
import cc.alcina.framework.entity.persistence.metric.InternalMetric;
import cc.alcina.framework.entity.persistence.transform.TransformCache;
import cc.alcina.framework.entity.persistence.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.util.MethodContext;

/**
 * @author Nick Reddel
 */
@Registration(CommonPersistenceBase.class)
public abstract class CommonPersistenceBase implements CommonPersistenceLocal {
	// note - this'll be a function of the stack depth of the eql ast processor
	private static final int PRECACHE_RQ_SIZE = 5000;

	public static final transient String CONTEXT_CLIENT_IP_ADDRESS = CommonPersistenceBase.class
			.getName() + ".CONTEXT_CLIENT_IP_ADDRESS";

	public static final transient String CONTEXT_CLIENT_INSTANCE_ID = CommonPersistenceBase.class
			.getName() + ".CONTEXT_CLIENT_INSTANCE_ID";

	public static final transient String CONTEXT_PROJECT_ENTITIES = CommonPersistenceBase.class
			.getName() + ".CONTEXT_PROJECT_ENTITIES";

	private static <A> Class<? extends A> getImplementation(Class<A> clazz) {
		return PersistentImpl.getImplementation(clazz);
	}

	private static String getImplementationSimpleClassName(Class<?> clazz) {
		return PersistentImpl.getImplementationSimpleClassName(clazz);
	}

	Logger logger = LoggerFactory.getLogger(CommonPersistenceBase.class);

	public CommonPersistenceBase() {
	}

	public CommonPersistenceBase(EntityManager em) {
		this();
		this.setEntityManager(em);
	}

	/**
	 * Note - parameter <em>fixWithPrecreate</em> will only be used in db
	 * replays for dbs which are somehow missing transform events
	 *
	 * @param onlyIfOptimal
	 */
	public DetachedEntityCache cacheEntities(List<DomainTransformEvent> items,
			boolean fixWithPrecreate, boolean onlyCacheIfWouldOptimiseCalls) {
		Multiset<Class, Set<Long>> lkp = new Multiset<Class, Set<Long>>();
		Multiset<Class, Set<Long>> creates = new Multiset<Class, Set<Long>>();
		DetachedEntityCache cache = new DetachedEntityCache();
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
			if (CommonUtils.compareWithNullMinusOne(precreDate,
					dte.getUtcDate()) > 0) {
				precreDate = dte.getUtcDate();
			}
		}
		for (Entry<Class, Set<Long>> entry : lkp.entrySet()) {
			Class storageClass = null;
			Class clazz = entry.getKey();
			List<Long> ids = new ArrayList<Long>(entry.getValue());
			if (clazz == null
					|| (ids.size() < 2 && onlyCacheIfWouldOptimiseCalls)) {
				// former means early, incorrect data - can be removed
				continue;
				// re 'onlyCacheIfWouldOptimiseCalls': no point making a call if
				// only
				// one of class (for
				// optimisation) - but must if we need the results for mixing
				// back into domainStore
			}
			if (clazz.getAnnotation(javax.persistence.Entity.class) != null) {
				storageClass = clazz;
			}
			if (storageClass != null) {
				for (int i = 0; i < ids.size(); i += PRECACHE_RQ_SIZE) {
					List<Long> idsSlice = ids.subList(i,
							Math.min(ids.size(), i + PRECACHE_RQ_SIZE));
					if (ids.size() > PRECACHE_RQ_SIZE) {
						logger.info("Transform precache - {} - {}",
								clazz.getSimpleName(),
								new IntPair(i, i + idsSlice.size()));
					}
					Query query = getEntityManager().createQuery(String.format(
							"from %s where id in %s",
							storageClass.getSimpleName(),
							EntityPersistenceHelper.toInClause(idsSlice)));
					query.setFlushMode(FlushModeType.COMMIT);
					List<Entity> resultList = query.getResultList();
					for (Entity entity : resultList) {
						cache.put(entity);
						if (fixWithPrecreate) {
							entry.getValue().remove(entity.getId());
						}
					}
				}
				if (fixWithPrecreate && storageClass == clazz) {
					entry.getValue().removeAll(creates.getAndEnsure(clazz));
					for (Long lv : entry.getValue()) {
						System.out.println(
								String.format("tp: create object: %10s %s", lv,
										clazz.getSimpleName()));
						ThreadlocalTransformManager.get().newInstance(clazz, lv,
								0);
					}
				}
			}
		}
		return cache;
	}

	@Override
	public <V> V
			callWithEntityManager(ThrowingFunction<EntityManager, V> call) {
		try {
			return call.apply(getEntityManager());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void changeJdbcConnectionUrl(String newUrl) {
		try {
			// try WF24 path
			String fieldPath = "jdbcConnectionAccess.connectionProvider.dataSource.delegate.cm.pool.mcf.connectionURL";
			ObjectUtil.setField(getEntityManager().getDelegate(), fieldPath,
					newUrl);
		} catch (Exception e) {
			// try legacy WF8 path
			String fieldPath = "emf.sessionFactory.jdbcServices.connectionProvider.dataSource.cm.pool.mcf.connectionURL";
			try {
				ObjectUtil.setField(getEntityManager(), fieldPath, newUrl);
			} catch (Exception e2) {
				throw new WrappedRuntimeException(e2);
			}
		}
	}

	protected abstract InstantiateImplCallback createUserAndGroupInstantiator();

	@Override
	public <T> T ensure(Class<T> clazz, String key, Object value) {
		String eql = Ax.format("from %s where %s = ?1", clazz.getSimpleName(),
				key);
		Query q = getEntityManager().createQuery(eql).setParameter(1, value);
		List list = q.getResultList();
		if (list.isEmpty()) {
			AppPersistenceBase.checkNotReadOnly();
			T instance = Reflections.newInstance(clazz);
			getEntityManager().persist(instance);
			Reflections.at(instance).property(key).set(instance, value);
			return instance;
		} else {
			Preconditions.checkState(list.size() == 1);
			return (T) list.get(0);
		}
	}

	@Override
	public void ensurePublicationCounters() {
		try (Connection conn = Registry
				.impl(CommonPersistenceConnectionProvider.class)
				.getConnection()) {
			Statement statement = conn.createStatement();
			statement.execute(
					"create index publicationcounter_user_id on publicationcounter using btree(user_id)");
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<Object[]> objects = getEntityManager().createQuery(Ax.format(
				"select max(userPublicationId), user.id from %s j group by user.id",
				getImplementationSimpleClassName(Publication.class)))
				.getResultList();
		for (Object[] array : objects) {
			long max = (long) array[0];
			long userId = (long) array[1];
			Class<? extends PublicationCounter> clazz = PersistentImpl
					.getImplementation(PublicationCounter.class);
			Class<? extends IUser> iUserClass = PersistentImpl
					.getImplementation(IUser.class);
			PublicationCounter counter = Reflections.newInstance(clazz);
			getEntityManager().persist(counter);
			IUser user = getEntityManager().find(iUserClass, userId);
			counter.setUser(user);
			counter.setCounter(max);
		}
		logger.info("Ensured {} publication counters", objects.size());
	}

	@Override
	public void expandExceptionInfo(DomainTransformLayerWrapper wrapper) {
		ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
		tm.resetTltm(wrapper.locatorMap);
		try {
			tm.setEntityManager(getEntityManager());
			for (DomainTransformException ex : wrapper.response
					.getTransformExceptions()) {
				tryAddSourceObjectName(ex);
			}
		} finally {
			tm.setEntityManager(null);
		}
	}

	@Override
	public <T> T findImplInstance(Class<? extends T> clazz, long id) {
		Class<?> implClazz = getImplementation(clazz);
		return (T) getEntityManager().find(implClazz, id);
	}

	public abstract EntityManager getEntityManager();

	@Override
	public Integer getHighestPersistedRequestIdForClientInstance(
			long clientInstanceId) {
		String eql = String.format(
				"select max(dtrq.requestId) as maxId "
						+ "from %s dtrq where dtrq.clientInstance.id=%s ",
				getImplementation(DomainTransformRequestPersistent.class)
						.getSimpleName(),
				clientInstanceId);
		Integer result = (Integer) getEntityManager().createQuery(eql)
				.getSingleResult();
		return result;
	}

	@Override
	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2) {
		String eql = String.format(
				"from %s where %s=?1 and %s=?2 order by id desc",
				clazz.getSimpleName(), key1, key2);
		Query q = getEntityManager().createQuery(eql).setParameter(1, value1)
				.setParameter(2, value2);
		List l = q.getResultList();
		return (T) ((l.size() == 0) ? null : l.get(0));
	}

	@Override
	public long getLastTransformId() {
		String eql = String.format("select max(dtep.id) from %s dtep ",
				getImplementationSimpleClassName(
						DomainTransformEventPersistent.class));
		Long l = (Long) getEntityManager().createQuery(eql).getSingleResult();
		return CommonUtils.lv(l);
	}

	@Override
	public EntityLocatorMap getLocatorMap(Long clientInstanceId) {
		// presumably null, but no harm in being light-on-the-ground
		EntityManager entryEntityManager = ThreadlocalTransformManager.get()
				.getEntityManager();
		EntityLocatorMap map;
		try {
			ThreadlocalTransformManager.get()
					.setEntityManager(getEntityManager());
			ThreadlocalTransformManager.get()
					.setClientInstanceEntityMap(new EntityLocatorMap());
			ClientInstance clientInstanceImpl = PersistentImpl
					.getNewImplementationInstance(ClientInstance.class);
			clientInstanceImpl.setId(clientInstanceId);
			// don't get the real client instance - don't want to attach
			// live permissions objects
			ThreadlocalTransformManager.get()
					.setClientInstance(clientInstanceImpl);
			map = ThreadlocalTransformManager.get().reconstituteEntityMap();
		} finally {
			ThreadlocalTransformManager.get()
					.setEntityManager(entryEntityManager);
		}
		return map;
	}

	@Override
	public /*
			 * Normally, transform trigger logic in DomainDescriptorPublication
			 * ensures that these are created on user creation - the fallback
			 * lock-and-create is for legacy systems.
			 */
	long getNextPublicationIdForUser(IUser user) {
		Query query = getEntityManager().createNativeQuery(
				"SELECT  id, counter from publicationCounter where user_id=?1 FOR UPDATE")
				.setParameter(1, user.getId());
		List<Object[]> resultList = query.getResultList();
		if (resultList.isEmpty()) {
			logger.warn("No publication counter for user {} - creating... ",
					user.toIdNameString());
			getEntityManager()
					.createNativeQuery("LOCK TABLE publicationCounter")
					.executeUpdate();
			resultList = query.getResultList();
			/*
			 * double-checked locking
			 */
			if (resultList.isEmpty()) {
				List<BigInteger> resultList2 = getEntityManager()
						.createNativeQuery(
								"SELECT nextval('publicationCounter_id_seq');")
						.getResultList();
				getEntityManager().createNativeQuery(
						"INSERT INTO publicationcounter (id,optlock,creationdate,lastmodificationdate,counter,user_id) VALUES (?1,?2,?3,?4,?5,?6);")
						.setParameter(1, resultList2.get(0).longValue())
						.setParameter(2, 1)
						.setParameter(3,
								new Timestamp(System.currentTimeMillis()))
						.setParameter(4,
								new Timestamp(System.currentTimeMillis()))
						.setParameter(5, 0).setParameter(6, user.getId())
						.executeUpdate();
			}
			resultList = query.getResultList();
		}
		long id = ((BigInteger) resultList.get(0)[0]).longValue();
		long counter = ((BigInteger) resultList.get(0)[1]).longValue();
		getEntityManager()
				.createNativeQuery(
						"update publicationCounter set counter=?1  where id=?2")
				.setParameter(1, ++counter).setParameter(2, id).executeUpdate();
		return counter;
	}

	@Override
	public List<DomainTransformRequestPersistent>
			getPersistentTransformRequests(long fromId, long toId,
					Collection<Long> specificIds, boolean mostRecentOnly,
					boolean populateTransformSourceObjects, Logger logger) {
		boolean logTransformReadMetrics = Configuration
				.is("logTransformReadMetrics");
		Query query = null;
		List<DomainTransformRequestPersistent> dtrps = null;
		if (mostRecentOnly) {
			DurationCounter dc = new DurationCounter();
			String eql = String.format(
					"select dte.domainTransformRequestPersistent.id  "
							+ "from %s dte " + "order by dte.id desc",
					getImplementation(DomainTransformEventPersistent.class)
							.getSimpleName(),
					fromId);
			query = getEntityManager().createQuery(eql);
			query.setMaxResults(1);
			List<Long> ids = query.getResultList();
			long id = ids.isEmpty() ? 0L : ids.get(0);
			dtrps = new ArrayList<DomainTransformRequestPersistent>();
			DomainTransformRequestPersistent instance = PersistentImpl
					.getNewImplementationInstance(
							DomainTransformRequestPersistent.class);
			instance.setId(id);
			dtrps.add(instance);
			if (logTransformReadMetrics) {
				dc.endWithLogger(logger, "dtrp-get-most-recent - %s ms");
			}
		} else {
			DurationCounter dc = new DurationCounter();
			String idFilter = specificIds == null
					? String.format("dtrp.id>=%s and dtrp.id<=%s", fromId, toId)
					: String.format("dtrp.id in %s",
							EntityPersistenceHelper.toInClause(specificIds));
			String eql = String.format(
					"select distinct dtrp " + "from %s dtrp "
							+ "inner join fetch dtrp.events "
							+ "inner join fetch dtrp.clientInstance "
							+ " where %s " + "order by dtrp.id",
					getImplementation(DomainTransformRequestPersistent.class)
							.getSimpleName(),
					idFilter);
			query = getEntityManager().createQuery(eql);
			dtrps = new ArrayList<DomainTransformRequestPersistent>(
					query.getResultList());
			if (logTransformReadMetrics) {
				dc.endWithLogger(logger, "dtrp-get-dtrps - %s ms");
			}
		}
		dtrps.stream().forEach(dtrp -> dtrp.getEvents().removeIf(
				DomainTransformEvent::provideNotApplicableToVmDomain));
		if (populateTransformSourceObjects) {
			DurationCounter dc = new DurationCounter();
			List<DomainTransformEvent> events = (List) DomainTransformRequest
					.allEvents(dtrps);
			DetachedEntityCache cache = cacheEntities(events, false, false);
			for (DomainTransformEvent event : events) {
				event.setSource((Entity) cache.get(event.getObjectClass(),
						event.getObjectId()));
			}
			if (logTransformReadMetrics) {
				dc.endWithLogger(logger,
						"populate transform source events - %s ms");
			}
		}
		DetachedEntityCache cache = new DetachedEntityCache();
		GraphProjectionDataFilter filter = Registry
				.impl(JPAImplementation.class)
				.getResolvingFilter(Registry.impl(JPAImplementation.class)
						.getClassrefInstantiator(), cache, true);
		GraphProjectionFieldFilter allowSourceFilter = new GraphProjectionFieldFilter() {
			@Override
			public Boolean permitClass(Class clazz) {
				return true;
			}

			@Override
			public boolean permitField(Field field,
					Set<Field> perObjectPermissionFields, Class clazz) {
				return true;
			}

			@Override
			public boolean permitTransient(Field field) {
				return field.getDeclaringClass() == DomainTransformEvent.class
						&& field.getName().equals("source");
			}
		};
		try {
			return new GraphProjection(allowSourceFilter, filter).project(dtrps,
					null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected Collection<Class> getPerUserTransformClasses() {
		return new ArrayList<Class>();
	}

	protected List<DomainTransformEventPersistent> getRecentTransforms(
			Collection<Class> sourceObjectClasses, int maxTransforms,
			long sinceId) {
		Set<Long> classRefIds = new HashSet<Long>();
		for (Class clazz : sourceObjectClasses) {
			ClassRef classRef = ClassRef.forClass(clazz);
			classRefIds.add(classRef.getId());
		}
		String eql = String.format(
				"select dtep from %s dtep "
						+ "where  dtep.id>?1 and dtep.objectClassRef.id in %s "
						+ "order by dtep.id desc",
				getImplementationSimpleClassName(
						DomainTransformEventPersistent.class),
				EntityPersistenceHelper.toInClause(classRefIds));
		Query query = getEntityManager().createQuery(eql).setParameter(1,
				sinceId);
		if (sinceId == 0) {
			query.setMaxResults(maxTransforms);
		}
		// unused, would just require a little tweak of the eql (removed as part
		// of EntityPersistenceHelper cleanup) to instantiate the classrefs
		throw new UnsupportedOperationException();
		// return new EntityPersistenceHelper().detachedClone(
		// query.getResultList(), Registry.impl(JPAImplementation.class)
		// .getClassrefInstantiator());
	}

	protected Collection<Class> getSharedTransformClasses() {
		return new ArrayList<Class>();
	}

	/**
	 * This is deliberately low - for big initial objects size, 1000 is quite
	 * reasonable (i.e. client load will be much faster)
	 */
	protected int getSharedTransformWarmupSize() {
		return 100;
	}

	@Override
	public List<Long> listRecentClientInstanceIds(String iidKey) {
		Class<? extends ClientInstance> clientInstanceImpl = (Class<? extends ClientInstance>) getImplementation(
				ClientInstance.class);
		return getEntityManager().createQuery(String.format(
				"select ci.id from %s ci where ci.iid = ?1 order by id desc",
				clientInstanceImpl.getSimpleName())).setParameter(1, iidKey)
				.setMaxResults(99).getResultList();
	}

	@Override
	public <E extends Entity> List<? extends E> listEntities(Class<E> clazz,
			long fromId, long toId) {
		Class<? extends E> implementationClass = PersistentImpl
				.getImplementationOrSelf(clazz);
		List resultList = getEntityManager().createQuery(String.format(
				"select  e from %s e where e.id >= ?1 and e.id < ?2 order by id desc",
				implementationClass.getSimpleName())).setParameter(1, fromId)
				.setParameter(2, toId).getResultList();
		if (LooseContext.is(CONTEXT_PROJECT_ENTITIES)) {
			GraphProjectionDataFilter dataFilter = Registry
					.impl(JPAImplementation.class)
					.getResolvingFilter(null, null, true);
			return GraphProjections.defaultProjections()
					.fieldFilter(Registry.impl(PermissibleFieldFilter.class))
					.maxDepth(2).dataFilter(dataFilter).project(resultList);
		} else {
			return resultList;
		}
	}

	@Override
	public long getMaxId(Class<? extends Entity> clazz) {
		Class<?> impl = (Class<?>) PersistentImpl
				.getImplementationOrSelf(clazz);
		List list = getEntityManager().createQuery(String
				.format("select  max(t.id) from %s t ", impl.getSimpleName()))
				.getResultList();
		return list.isEmpty() ? 0L : (long) list.get(0);
	}

	@Override
	public long log(String message, String componentKey) {
		// not required...useful but
		return 0;
	}

	@Override
	public long log(String message, String componentKey, String data) {
		// not required...useful but
		return 0;
	}

	@Override
	public void persistClientLogRecords(List<ClientLogRecords> recordsList) {
		List<ClientLogRecord> records = new ArrayList<ClientLogRecord>();
		for (ClientLogRecords r : recordsList) {
			records.addAll(r.getLogRecords());
		}
		for (ClientLogRecord clr : records) {
			ClientLogRecordPersistent clrp = PersistentImpl
					.getNewImplementationInstance(
							ClientLogRecordPersistent.class);
			getEntityManager().persist(clrp);
			clrp.wrap(clr);
		}
	}

	@Override
	public void persistInternalMetrics(List<InternalMetric> toPersist) {
		if (AppPersistenceBase.isInstanceReadOnly()) {
			return;
		}
		for (InternalMetric metric : toPersist) {
			if (metric.getId() != 0) {
				InternalMetric managed = getEntityManager()
						.find(metric.getClass(), metric.getId());
				metric.setVersionNumber(managed.getVersionNumber());
				getEntityManager().merge(metric);
			} else {
				getEntityManager().persist(metric);
			}
		}
	}

	@Override
	public void ping() {
		MethodContext.instance().withMetricKey("ping").run(() -> {
			List list = getEntityManager().createNativeQuery("select 1")
					.getResultList();
			Ax.out(list.get(0));
		});
	}

	protected SearchResultsBase
			projectSearchResults(SearchResultsBase results) {
		return DomainLinker.linkToDomain(results);
	}

	@Override
	public EntityLocatorMap reconstituteEntityMap(long clientInstanceId) {
		ThreadlocalTransformManager tltm = new ThreadlocalTransformManager();
		tltm.resetTltm(new EntityLocatorMap());
		EntityLocatorMap result;
		try {
			tltm.setEntityManager(getEntityManager());
			ClientInstance clientInstance = PersistentImpl
					.getNewImplementationInstance(ClientInstance.class);
			clientInstance.setId(clientInstanceId);
			// don't use the real client instance, requires
			// permissionsmanager>>liveobjects
			tltm.setClientInstance(clientInstance);
			result = tltm.reconstituteEntityMap();
			return result;
		} finally {
			tltm.resetTltm(null);
		}
	}

	@Override
	public /**
			 * return true if has non-zero unprocessed requests
			 */
	boolean removeProcessedRequests(
			TransformPersistenceToken persistenceToken) {
		return new TransformPersisterInPersistenceContext()
				.removeProcessedRequests(this, persistenceToken);
	}

	@Override
	public SearchResultsBase search(SearchDefinition def) {
		String message = def.validatePermissions();
		if (message != null) {
			throw new WrappedRuntimeException(
					new PermissionsException(message));
		}
		Searcher searcher = Registry.impl(Searcher.class, def.getClass());
		SearchResultsBase result = searcher.search(def, getEntityManager());
		return projectSearchResults(result);
	}

	public abstract void setEntityManager(EntityManager entityManager);

	@Override
	public void setField(Class clazz, Long id, String key, Object value)
			throws Exception {
		AppPersistenceBase.checkNotReadOnly();
		Object inst = getEntityManager().find(clazz, id);
		PropertyDescriptor descriptor = SEUtilities
				.getPropertyDescriptorByName(clazz, key);
		descriptor.getWriteMethod().invoke(inst, value);
		getEntityManager().merge(inst);
	}

	@Override
	public DomainTransformLayerWrapper transformInPersistenceContext(
			TransformPersisterToken transformPersisterToken,
			TransformPersistenceToken token,
			DomainTransformLayerWrapper wrapper) {
		AppPersistenceBase.checkNotReadOnly();
		new TransformPersisterInPersistenceContext()
				.transformInPersistenceContext(transformPersisterToken, token,
						this, getEntityManager(), wrapper);
		return wrapper;
	}

	private void tryAddSourceObjectName(
			DomainTransformException transformException) {
		if (transformException.getEvent() == null) {
			return;
		}
		try {
			Entity object = TransformManager.get()
					.getObject(transformException.getEvent(), true);
			if (object != null) {
				transformException.setSourceObjectName(
						HasDisplayName.displayName(object));
			}
		} catch (Exception e) {
			System.out.println("Unable to add source object name - reason: "
					+ e.getMessage());
			// we tried
			// e.printStackTrace();
		}
	}

	@Override
	public void updatePublicationMimeMessageId(Long publicationId,
			String mimeMessageId) {
		getEntityManager()
				.createQuery(
						Ax.format("update %s set mimeMessageId=?1 where id=?2",
								getImplementationSimpleClassName(
										Publication.class)))
				.setParameter(1, mimeMessageId).setParameter(2, publicationId)
				.executeUpdate();
	}

	/**
	 * Used for supporting mixed rpc/transform domain loads
	 *
	 * @param userId
	 */
	@Override
	public TransformCache warmupTransformCache() {
		TransformCache result = new TransformCache();
		List<DomainTransformEventPersistent> recentTransforms = getRecentTransforms(
				getSharedTransformClasses(), getSharedTransformWarmupSize(),
				0L);
		result.sharedTransformClasses = getSharedTransformClasses();
		result.perUserTransformClasses = getPerUserTransformClasses();
		if (!recentTransforms.isEmpty()) {
			result.putSharedTransforms(recentTransforms);
			recentTransforms = getRecentTransforms(getPerUserTransformClasses(),
					0, result.cacheValidFrom);
			result.putPerUserTransforms(recentTransforms);
		} else {
			result.invalid = true;
		}
		return result;
	}

	@Registration(CommonPersistenceConnectionProvider.class)
	public abstract static class CommonPersistenceConnectionProvider {
		public abstract Connection getConnection();
	}

	@Override
	public void authenticationResetIid(long id) {
		String eql = Ax.format("update iid set optlock=0 where id=%s", id);
		getEntityManager().createNativeQuery(eql).executeUpdate();
	}

	@Override
	public BootstrapCreationResult
			authenticationCreateBootstrapClientInstance(String hostName) {
		EntityManager em = getEntityManager();
		BootstrapCreationResult result = new BootstrapCreationResult();
		String authenticationSessionUid = Ax.format("%s%s",
				ClientInstance.SERVLET_PREFIX, hostName);
		String iidUid = authenticationSessionUid;
		List<Entity> createdObjects = new ArrayList<>();
		Iid iid = (Iid) Ax.first(em.createQuery(Ax.format(
				"select iid from %s iid where instanceId = '%s'",
				PersistentImpl.getImplementationSimpleClassName(Iid.class),
				iidUid)).getResultList());
		if (iid == null) {
			iid = PersistentImpl.getNewImplementationInstance(Iid.class);
			iid.setInstanceId(iidUid);
			em.persist(iid);
			createdObjects.add(iid);
		}
		AuthenticationSession authenticationSession = (AuthenticationSession) Ax
				.first(em.createQuery(Ax.format(
						"select authenticationSession from %s authenticationSession where sessionId = '%s'",
						PersistentImpl.getImplementationSimpleClassName(
								AuthenticationSession.class),
						authenticationSessionUid)).getResultList());
		if (authenticationSession == null) {
			authenticationSession = PersistentImpl
					.getNewImplementationInstance(AuthenticationSession.class);
			authenticationSession.setSessionId(authenticationSessionUid);
			authenticationSession.setStartTime(new Date());
			authenticationSession.setUser((IUser) persistentImpl(em,
					(Entity) Permissions.get().getUser()));
			authenticationSession.setAuthenticationType("server-instance");
			authenticationSession.setIid(iid);
			em.persist(authenticationSession);
			createdObjects.add(authenticationSession);
		}
		ClientInstance clientInstance = PersistentImpl
				.getNewImplementationInstance(ClientInstance.class);
		clientInstance.setHelloDate(new Date());
		clientInstance.setUserAgent(authenticationSessionUid);
		clientInstance.setAuthenticationSession(authenticationSession);
		clientInstance.setAuth(Math.abs(new Random().nextInt()));
		clientInstance.setIpAddress("127.0.0.1");
		clientInstance.setBotUserAgent(false);
		em.persist(clientInstance);
		Iid detachedIid = PersistentImpl
				.getNewImplementationInstance(Iid.class);
		detachedIid.setId(iid.getId());
		if (createdObjects.contains(iid)) {
			result.createdDetached.add(detachedIid);
		}
		AuthenticationSession detachedSession = PersistentImpl
				.getNewImplementationInstance(AuthenticationSession.class);
		detachedSession.setId(authenticationSession.getId());
		detachedSession.setIid(detachedIid);
		if (createdObjects.contains(authenticationSession)) {
			result.createdDetached.add(detachedSession);
		}
		ClientInstance detachedInstance = PersistentImpl
				.getNewImplementationInstance(ClientInstance.class);
		detachedInstance.setId(clientInstance.getId());
		detachedInstance.setAuthenticationSession(detachedSession);
		result.createdDetached.add(detachedInstance);
		result.clientInstance = detachedInstance;
		return result;
	}

	<V extends Entity> V persistentImpl(EntityManager em, V v) {
		return (V) em.find(v.entityClass(), v.getId());
	}

	public Long authenticationGetAuthenticationSessionId(String sessionId) {
		Class<? extends AuthenticationSession> clazz = PersistentImpl
				.getImplementation(AuthenticationSession.class);
		String query = Ax.format(
				"select authenticationSession.id from %s authenticationSession where %s='%s'",
				clazz.getSimpleName(), "sessionId", sessionId);
		return Ax.first((List<Long>) getEntityManager().createQuery(query)
				.getResultList());
	}

	public Long authenticationGetIidId(String instanceId) {
		Class<? extends Iid> clazz = PersistentImpl
				.getImplementation(Iid.class);
		String query = Ax.format("select id from %s where %s='%s'",
				clazz.getSimpleName(), "instanceId", instanceId);
		return Ax.first((List<Long>) getEntityManager().createQuery(query)
				.getResultList());
	}
}
