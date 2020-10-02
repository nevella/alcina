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
package cc.alcina.framework.entity.entityaccess;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.entity.ClientLogRecordPersistent;
import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerUniquenessValidator;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.Wrapper;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DurationCounter;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.WrappedObjectProvider;
import cc.alcina.framework.entity.entityaccess.UnwrapInfoItem.UnwrapInfoContainer;
import cc.alcina.framework.entity.entityaccess.cache.DomainLinker;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Mvcc;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccObject;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetric;
import cc.alcina.framework.entity.entityaccess.transform.TransformCache;
import cc.alcina.framework.entity.entityaccess.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.entityaccess.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionFieldFilter;
import cc.alcina.framework.entity.projection.GraphProjection.InstantiateImplCallback;
import cc.alcina.framework.entity.util.MethodContext;

/**
 *
 * @author Nick Reddel
 * 
 *         FIXME - mvcc.4 - de-generify; remove getitemby methods
 */
public abstract class CommonPersistenceBase implements CommonPersistenceLocal {
	// note - this'll be a function of the stack depth of the eql ast processor
	private static final int PRECACHE_RQ_SIZE = 500;

	public static final transient String CONTEXT_CLIENT_IP_ADDRESS = CommonPersistenceBase.class
			.getName() + ".CONTEXT_CLIENT_IP_ADDRESS";

	public static final transient String CONTEXT_CLIENT_INSTANCE_ID = CommonPersistenceBase.class
			.getName() + ".CONTEXT_CLIENT_INSTANCE_ID";

	private static <A> Class<? extends A> getImplementation(Class<A> clazz) {
		return AlcinaPersistentEntityImpl.getImplementation(clazz);
	}

	private static String getImplementationSimpleClassName(Class<?> clazz) {
		return AlcinaPersistentEntityImpl
				.getImplementationSimpleClassName(clazz);
	}

	Map<Long, EntityLocatorMap> locatorMaps = Collections
			.synchronizedMap(new LinkedHashMap<>());

	public CommonPersistenceBase() {
		ObjectPersistenceHelper.get();
	}

	public CommonPersistenceBase(EntityManager em) {
		this();
		this.setEntityManager(em);
	}

	/**
	 * An implementation can perform a (direct sql) accelerated delete - but you
	 * may want to avoid this if the entities to delete require delete/cascade
	 */
	@Override
	public void bulkDelete(Class clazz, Collection<Long> ids, boolean tryImpl) {
		AppPersistenceBase.checkNotReadOnly();
		if (!tryImpl || !Registry.impl(JPAImplementation.class)
				.bulkDelete(getEntityManager(), clazz, ids)) {
			List<Object> resultList = getEntityManager()
					.createQuery(String.format("from %s where id in %s ",
							clazz.getSimpleName(),
							EntityPersistenceHelper.toInClause(ids)))
					.getResultList();
			for (Object object : resultList) {
				getEntityManager().remove(object);
			}
		}
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
				continue; // former means early, incorrect data - can be removed
				// re 'onlyCacheIfWouldOptimiseCalls': no point making a call if
				// only
				// one of class (for
				// optimisation) - but must if we need the results for mixing
				// back into domainStore
			}
			if (clazz.getAnnotation(javax.persistence.Entity.class) != null) {
				storageClass = clazz;
			}
			if (WrapperPersistable.class.isAssignableFrom(clazz)) {
				storageClass = getImplementation(WrappedObject.class);
			}
			if (storageClass != null) {
				for (int i = 0; i < ids.size(); i += PRECACHE_RQ_SIZE) {
					List<Long> idsSlice = ids.subList(i,
							Math.min(ids.size(), i + PRECACHE_RQ_SIZE));
					List<Entity> resultList = getEntityManager()
							.createQuery(String.format("from %s where id in %s",
									storageClass.getSimpleName(),
									EntityPersistenceHelper
											.toInClause(idsSlice)))
							.getResultList();
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
	public <V> V callWithEntityManager(Function<EntityManager, V> call) {
		return call.apply(getEntityManager());
	}

	@Override
	public void changeJdbcConnectionUrl(String newUrl) {
		String fieldPath = "emf.sessionFactory.jdbcServices.connectionProvider.dataSource.cm.pool.mcf.connectionURL";
		try {
			ResourceUtilities.setField(getEntityManager(), fieldPath, newUrl);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public <T> T ensureObject(T t, String key, String value) throws Exception {
		T newT = (T) getItemByKeyValue(t.getClass(), key, value, false);
		if (newT != null) {
			return newT;
		}
		AppPersistenceBase.checkNotReadOnly();
		PropertyDescriptor descriptor = SEUtilities
				.getPropertyDescriptorByName(t.getClass(), key);
		descriptor.getWriteMethod().invoke(t, value);
		return getEntityManager().merge(t);
	}

	@Override
	public <T extends HasId> T ensurePersistent(T obj) {
		if (!(obj instanceof MvccObject) && getEntityManager().contains(obj)) {
			return obj;
		}
		return (T) getEntityManager()
				.find(Mvcc.resolveEntityClass(obj.getClass()), obj.getId());
	}

	@Override
	public void expandExceptionInfo(DomainTransformLayerWrapper wrapper) {
		ThreadlocalTransformManager tm = ThreadlocalTransformManager.cast();
		tm.resetTltm(wrapper.locatorMap);
		tm.setEntityManager(getEntityManager());
		for (DomainTransformException ex : wrapper.response
				.getTransformExceptions()) {
			tryAddSourceObjectName(ex);
		}
	}

	@Override
	public <T> T findImplInstance(Class<? extends T> clazz, long id) {
		Class<?> implClazz = getImplementation(clazz);
		return (T) getEntityManager().find(implClazz, id);
	}

	@Override
	public <A> Set<A> getAll(Class<A> clazz) {
		Query query = getEntityManager()
				.createQuery(String.format("from %s ", clazz.getSimpleName()));
		List results = query.getResultList();
		return new LinkedHashSet<A>(results);
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
	public <T> T getItemById(Class<T> clazz, Long id) {
		return getItemById(clazz, id, false, false);
	}

	@Override
	public <T> T getItemById(Class<T> clazz, Long id, boolean clean,
			boolean unwrap) {
		T t = getEntityManager().find(clazz, id);
		if (t == null) {
			return t;
		}
		if (clean) {
			t = DomainLinker.linkToDomain(t);
		}
		if (unwrap) {
			try {
				PermissionsManager.get().pushCurrentUser();
				unwrap((HasId) t);
			} finally {
				PermissionsManager.get().popUser();
			}
		}
		return t;
	}

	@Override
	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent) {
		return getItemByKeyValue(clazz, key, value, createIfNonexistent, null,
				false);
	}

	@Override
	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent, Long ignoreId,
			boolean caseInsensitive) {
		boolean hadException = false;
		try {
			String eql = String.format(
					value == null ? "from %s where %s is null"
							: caseInsensitive ? "from %s where lower(%s) = ?1"
									: "from %s where %s = ?1",
					clazz.getSimpleName(), key);
			if (ignoreId != null) {
				eql += " AND id != " + ignoreId;
			}
			if (HasId.class.isAssignableFrom(clazz)) {
				eql += " order by id asc";
			}
			Query q = getEntityManager().createQuery(eql);
			if (value != null) {
				q.setParameter(1,
						caseInsensitive ? value.toString().toLowerCase()
								: value);
			}
			List l = q.getResultList();
			if (l.size() == 0 && createIfNonexistent) {
				AppPersistenceBase.checkNotReadOnly();
				T inst = clazz.newInstance();
				getEntityManager().persist(inst);
				PropertyDescriptor descriptor = SEUtilities
						.getPropertyDescriptorByName(inst.getClass(), key);
				descriptor.getWriteMethod().invoke(inst, value);
				return inst;
			}
			return (T) ((l.size() == 0) ? null : l.get(0));
		} catch (Exception e) {
			hadException = true;
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public <T> T getItemByKeyValueKeyValue(Class<T> clazz, String key1,
			Object value1, String key2, Object value2) {
		String eql = String.format("from %s where %s=?1 and %s=?2",
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
		EntityManager cachedEntityManager = ThreadlocalTransformManager.get()
				.getEntityManager();
		ThreadlocalTransformManager.get().setEntityManager(getEntityManager());
		ThreadlocalTransformManager.get()
				.setUserSessionEntityMap(new EntityLocatorMap());
		ClientInstance clientInstanceImpl = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(ClientInstance.class);
		clientInstanceImpl.setId(clientInstanceId);
		// don't get the real client instance - don't want to attach
		// live permissions objects
		ThreadlocalTransformManager.get().setClientInstance(clientInstanceImpl);
		EntityLocatorMap map = ThreadlocalTransformManager.get()
				.reconstituteEntityMap();
		ThreadlocalTransformManager.get().setEntityManager(cachedEntityManager);
		return map;
	}

	@Override
	public long getMaxPublicationIdForUser(IUser user) {
		List<Long> longs = getEntityManager()
				.createQuery(Ax.format(
						"select userPublicationId from %s j where user = ?"
								+ " order by id desc",
						getImplementationSimpleClassName(Publication.class)))
				.setParameter(1, user).setMaxResults(1).getResultList();
		long maxId = longs.isEmpty() ? 0 : longs.get(0);
		return maxId;
	}

	@Override
	public LongPair getMinMaxIdRange(Class clazz) {
		Class implClass = getImplementation(clazz);
		clazz = implClass == null ? clazz : implClass;
		String eql = String.format("select min(id),max(id) from %s",
				clazz.getSimpleName());
		Object[] result = (Object[]) getEntityManager().createQuery(eql)
				.getSingleResult();
		return result[0] == null ? new LongPair()
				: new LongPair((Long) result[0], (Long) result[1]);
	}

	@Override
	public Date getMostRecentClientInstanceCreationDate(IUser o) {
		Class<? extends ClientInstance> clientInstanceImpl = (Class<? extends ClientInstance>) getImplementation(
				ClientInstance.class);
		List<Date> resultList = getEntityManager().createQuery(String.format(
				"select ci.helloDate from %s ci where ci.user.id = ?1 order by id desc",
				clientInstanceImpl.getSimpleName())).setParameter(1, o.getId())
				.setMaxResults(1).getResultList();
		return CommonUtils.first(resultList);
	}

	@Override
	public <T extends WrapperPersistable> WrappedObject<T>
			getObjectWrapperForUser(Class<T> c, long id) throws Exception {
		return getObjectWrapperForUser(null, c, id);
	}

	public <T extends WrapperPersistable> WrappedObject<T>
			getObjectWrapperForUser(HasId wrapperOwner, Class<T> c, long id)
					throws Exception {
		WrappedObject<T> wrapper = Registry.impl(WrappedObjectProvider.class)
				.getObjectWrapperForUser(c, id, getEntityManager());
		checkWrappedObjectAccess(wrapperOwner, wrapper, c);
		return wrapper;
	}

	@Override
	public List<DomainTransformRequestPersistent>
			getPersistentTransformRequests(long fromId, long toId,
					Collection<Long> specificIds, boolean mostRecentOnly,
					boolean populateTransformSourceObjects, Logger logger) {
		boolean logTransformReadMetrics = ResourceUtilities
				.is(CommonPersistenceBase.class, "logTransformReadMetrics");
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
			DomainTransformRequestPersistent instance = AlcinaPersistentEntityImpl
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

	@Override
	public Publication getPublication(long id) {
		return CommonUtils
				.first(getPublications(Collections.singletonList(id)));
	}

	@Override
	public List<Publication> getPublications(Collection<Long> ids) {
		String sql = Ax.format(
				"select pub from %s pub where id in %s order by id",
				getImplementationSimpleClassName(Publication.class),
				EntityPersistenceHelper.toInClause(ids));
		List<Publication> publications = getEntityManager().createQuery(sql)
				.getResultList();
		unwrap(publications);
		return DomainLinker.linkToDomain(publications);
	}

	@Override
	public <T extends WrapperPersistable> T getWrappedObjectForUser(
			Class<? extends T> c, long id) throws Exception {
		T wofu = (T) Registry.impl(WrappedObjectProvider.class)
				.getWrappedObjectForUser(c, id, getEntityManager());
		return (T) wofu;
	}

	@Override
	public List<ActionLogItem> listLogItemsForClass(String className,
			int count) {
		List list = getEntityManager()
				.createQuery("from "
						+ getImplementationSimpleClassName(ActionLogItem.class)
						+ " a where a.actionClassName=?1 order"
						+ " by a.actionDate DESC")
				.setParameter(1, className).setMaxResults(count)
				.getResultList();
		return DomainLinker.linkToDomain(list);
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
	// FIXME - mvcc.4 - persist with transforms
	public void logActionItem(ActionLogItem actionItem) {
		AppPersistenceBase.checkNotReadOnly();
		getEntityManager().merge(actionItem);
	}

	@Override
	// FIXME - mvcc.4 - persist with transforms
	public long merge(HasId hi) {
		AppPersistenceBase.checkNotReadOnly();
		if (hi instanceof Publication) {
			((Publication) hi).setUser(
					getEntityManager().find(getImplementation(IUser.class),
							((Publication) hi).getUser().getId()));
		}
		persistWrappables(hi);
		HasId merge = getEntityManager().merge(hi);
		return merge.getId();
	}

	@Override
	// FIXME - mvcc.4 - persist with transforms
	public IUser mergeUser(IUser user) {
		AppPersistenceBase.checkNotReadOnly();
		IUser merge = getEntityManager().merge(user);
		getEntityManager().flush();
		return merge;
	}

	@Override
	public <WP extends WrapperPersistable> Long persist(WP gwpo)
			throws Exception {
		AppPersistenceBase.checkNotReadOnly();
		WrappedObject<WP> wrapper = (WrappedObject<WP>) getObjectWrapperForUser(
				gwpo.getClass(), gwpo.getId());
		wrapper.setObject(gwpo);
		return wrapper.getId();
	}

	@Override
	public void persistClientLogRecords(List<ClientLogRecords> recordsList) {
		List<ClientLogRecord> records = new ArrayList<ClientLogRecord>();
		for (ClientLogRecords r : recordsList) {
			records.addAll(r.getLogRecords());
		}
		for (ClientLogRecord clr : records) {
			ClientLogRecordPersistent clrp = AlcinaPersistentEntityImpl
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

	@Override
	public UnwrapInfoContainer prepareUnwrap(Class<? extends HasId> clazz,
			Long id) {
		HasId wrapper = getItemById(clazz, id);
		if (wrapper == null) {
			return null;
		}
		UnwrapInfoContainer result = new UnwrapInfoContainer();
		result.setHasId(wrapper);
		try {
			PropertyDescriptor[] pds = Introspector
					.getBeanInfo(wrapper.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null) {
					Wrapper info = pd.getReadMethod()
							.getAnnotation(Wrapper.class);
					if (info != null) {
						PropertyDescriptor idpd = SEUtilities
								.getPropertyDescriptorByName(wrapper.getClass(),
										info.idPropertyName());
						Long wrapperId = (Long) idpd.getReadMethod().invoke(
								wrapper, CommonUtils.EMPTY_OBJECT_ARRAY);
						if (wrapperId != null) {
							Class<? extends WrapperPersistable> pType = (Class<? extends WrapperPersistable>) pd
									.getPropertyType();
							if (info.defaultImplementationType() != Void.class) {
								pType = info.defaultImplementationType();
							}
							WrappedObject wrappedObject = (WrappedObject) getObjectWrapperForUser(
									wrapper, pType, wrapperId);
							result.getItems().add(new UnwrapInfoItem(
									pd.getName(), wrappedObject));
						}
					}
				}
			}
			return DomainLinker.linkToDomain(result);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public EntityLocatorMap reconstituteEntityMap(long clientInstanceId) {
		ThreadlocalTransformManager tm = new ThreadlocalTransformManager();
		tm.resetTltm(new EntityLocatorMap());
		tm.setEntityManager(getEntityManager());
		ClientInstance clientInstance = AlcinaPersistentEntityImpl
				.getNewImplementationInstance(ClientInstance.class);
		clientInstance.setId(clientInstanceId);
		// don't use the real client instance, requires
		// permissionsmanager>>liveobjects
		tm.setClientInstance(clientInstance);
		EntityLocatorMap result = tm.reconstituteEntityMap();
		tm.resetTltm(null);
		return result;
	}

	@Override
	public void remove(Object o) {
		AppPersistenceBase.checkNotReadOnly();
		if (o instanceof Entity) {
			Entity entity = (Entity) getEntityManager().find(o.getClass(),
					((Entity) o).getId());
			getEntityManager().remove(entity);
		} else {
			throw new RuntimeException(
					"Cannot remove detached non-entity " + o);
		}
	}

	@Override
	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		String message = def.validatePermissions();
		if (message != null) {
			throw new WrappedRuntimeException(
					new PermissionsException(message));
		}
		Searcher searcher = (Searcher) Registry.get()
				.instantiateSingle(Searcher.class, def.getClass());
		SearchResultsBase result = searcher.search(def, pageNumber,
				getEntityManager());
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

	@Override
	// permissions...? (well, it's going away with FIXME - mvcc.wrapped)
	public <T extends HasId> Collection<T> unwrap(Collection<T> wrappers) {
		preloadWrappedObjects(wrappers);
		RuntimeException lastException = null;
		for (HasId wrapper : wrappers) {
			try {
				unwrap(wrapper);
			} catch (RuntimeException e) {
				System.out.println(e.getMessage());
				lastException = e;
			}
		}
		if (lastException != null) {
			throw lastException;
		}
		return wrappers;
	}

	@Override
	public HasId unwrap(HasId wrapper) {
		try {
			new WrappedObjectPersistence().unwrap(wrapper, getEntityManager(),
					Registry.impl(WrappedObjectProvider.class));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return wrapper;
	}

	@Override
	public void updatePublicationMimeMessageId(Long publicationId,
			String mimeMessageId) {
		getEntityManager()
				.createQuery(
						Ax.format("update %s set mimeMessageId=? where id=?",
								getImplementationSimpleClassName(
										Publication.class)))
				.setParameter(1, mimeMessageId).setParameter(2, publicationId)
				.executeUpdate();
	}

	@Override
	public <T extends ServerValidator> List<T> validate(List<T> validators) {
		ArrayList<T> result = new ArrayList<T>();
		for (T serverValidator : validators) {
			if (serverValidator instanceof ServerUniquenessValidator) {
				ServerUniquenessValidator suv = (ServerUniquenessValidator) serverValidator;
				int ctr = 0;
				String value = suv.getValue();
				suv.setSuggestedValue(value);
				while (true) {
					Object item = getItemByKeyValue(suv.getObjectClass(),
							suv.getPropertyName(), value, false, suv.getOkId(),
							suv.isCaseInsensitive());
					if (item == null) {
						if (ctr != 0) {
							suv.setSuggestedValue(value);
							suv.setMessage(
									"Item exists. Suggested value: " + value);
						}
						break;
					}
					// no suggestions, just error
					if (suv.getValueTemplate() == null) {
						suv.setMessage("Item exists");
						break;
					}
					ctr++;
					value = String.format(suv.getValueTemplate(),
							suv.getValue() == null ? "" : suv.getValue(), ctr);
				}
			} else {
				Class c = EntityLayerObjects.get().getServletLayerRegistry()
						.lookupSingle(ServerValidator.class,
								serverValidator.getClass());
				if (c != null) {
					ServerValidatorHandler handler = (ServerValidatorHandler) EntityLayerObjects
							.get().getServletLayerRegistry()
							.instantiateSingle(ServerValidator.class,
									serverValidator.getClass());
					handler.handle(serverValidator, getEntityManager());
				}
			}
			result.add(serverValidator);
		}
		return result;
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

	private void persistWrappables(HasId hi) {
		AppPersistenceBase.checkNotReadOnly();
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(hi.getClass())
					.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() != null) {
					Wrapper info = pd.getReadMethod()
							.getAnnotation(Wrapper.class);
					if (info != null) {
						Object obj = pd.getReadMethod().invoke(hi,
								CommonUtils.EMPTY_OBJECT_ARRAY);
						if (obj instanceof WrapperPersistable) {
							if (!(GwtMultiplePersistable.class
									.isAssignableFrom(obj.getClass()))) {
								throw new Exception(
										"Trying to persist a per-user object via wrapping");
							}
							WrapperPersistable gwpo = (WrapperPersistable) obj;
							if (info.toStringPropertyName().length() != 0) {
								PropertyDescriptor tspd = SEUtilities
										.getPropertyDescriptorByName(
												hi.getClass(),
												info.toStringPropertyName());
								tspd.getWriteMethod().invoke(hi,
										gwpo.toString());
							}
							Long persistId = persist(gwpo);
							PropertyDescriptor idpd = SEUtilities
									.getPropertyDescriptorByName(hi.getClass(),
											info.idPropertyName());
							idpd.getWriteMethod().invoke(hi, persistId);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private <T extends HasId> void
			preloadWrappedObjects(Collection<T> wrappers) {
		try {
			List<Long> wrapperIds = new WrappedObjectPersistence()
					.getWrapperIds(wrappers);
			for (int i = 0; i < wrapperIds.size(); i += PRECACHE_RQ_SIZE) {
				List<Long> subList = wrapperIds.subList(i,
						Math.min(wrapperIds.size(), i + PRECACHE_RQ_SIZE));
				Query query = getEntityManager()
						.createQuery("from "
								+ getImplementation(WrappedObject.class)
										.getSimpleName()
								+ " where id in "
								+ EntityPersistenceHelper.toInClause(subList));
				query.getResultList();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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
						Reflections.classLookup().displayNameForObject(object));
			}
		} catch (Exception e) {
			System.out.println("Unable to add source object name - reason: "
					+ e.getMessage());
			// we tried
			// e.printStackTrace();
		}
	}

	protected void checkWrappedObjectAccess(HasId wrapper,
			WrappedObject wrapped, Class clazz) throws PermissionsException {
		new WrappedObjectPersistence().checkWrappedObjectAccess(wrapper,
				wrapped, clazz);
	}

	protected abstract InstantiateImplCallback createUserAndGroupInstantiator();

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

	protected String getUserNamePropertyName() {
		return "userName";
	}

	protected SearchResultsBase
			projectSearchResults(SearchResultsBase results) {
		return DomainLinker.linkToDomain(results);
	}

	/**
	 * Note that this is only for in-tx work - normally use the DomainStore
	 * access
	 */
	IUser getUserByName(String userName) {
		List<IUser> list = getEntityManager()
				.createQuery(
						String.format(
								"select distinct u from "
										+ getImplementationSimpleClassName(
												IUser.class)
										+ " u " + "where u.%s = ?1",
								getUserNamePropertyName()))
				.setParameter(1, userName).getResultList();
		return Ax.first(list);
	}

	@RegistryLocation(registryPoint = CommonPersistenceConnectionProvider.class)
	public abstract static class CommonPersistenceConnectionProvider {
		public abstract Connection getConnection();
	}
}
